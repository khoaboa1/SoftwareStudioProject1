package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.email.EmailSender;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    private static final int MAX_PIN_ATTEMPTS = 5;

    /** One hour rolling window for counting PIN sends per student. */
    private static final Duration SEND_WINDOW = Duration.ofHours(1);

    private final StudentRepository studentRepository;
    private final List<EmailSender> emailSenders;
    private final UniversityEmailValidator universityEmailValidator;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean logPin;
    private final Duration resendCooldown;
    private final int maxSendsPerHour;
    private final Clock clock;

    /** Spring injects every EmailSender bean, sorted by {@code @Order}. */
    public VerificationService(StudentRepository studentRepository, List<EmailSender> emailSenders,
            UniversityEmailValidator universityEmailValidator) {
        this(studentRepository, emailSenders, universityEmailValidator, false);
    }

    public VerificationService(StudentRepository studentRepository, List<EmailSender> emailSenders,
            UniversityEmailValidator universityEmailValidator, boolean logPin) {
        this(studentRepository, emailSenders, universityEmailValidator, logPin, Duration.ofSeconds(60), 5,
                Clock.systemDefaultZone());
    }

    @Autowired
    public VerificationService(StudentRepository studentRepository, List<EmailSender> emailSenders,
            UniversityEmailValidator universityEmailValidator, @Value("${auth.log-pin:false}") boolean logPin,
            @Value("${auth.pin.resend-cooldown-seconds:60}") long resendCooldownSeconds,
            @Value("${auth.pin.max-sends-per-hour:5}") int maxSendsPerHour) {
        this(studentRepository, emailSenders, universityEmailValidator, logPin,
                Duration.ofSeconds(resendCooldownSeconds), maxSendsPerHour, Clock.systemDefaultZone());
    }

    VerificationService(StudentRepository studentRepository, List<EmailSender> emailSenders,
            UniversityEmailValidator universityEmailValidator, boolean logPin, Duration resendCooldown,
            int maxSendsPerHour, Clock clock) {
        this.studentRepository = studentRepository;
        this.emailSenders = emailSenders;
        this.universityEmailValidator = universityEmailValidator;
        this.logPin = logPin;
        this.resendCooldown = resendCooldown;
        this.maxSendsPerHour = maxSendsPerHour;
        this.clock = clock;
    }

    public String generateAndSendPin(Student student) {
        if (!universityEmailValidator.isValid(student.getEmail())) {
            throw new InvalidSignupException("Only .edu school emails can receive a verification PIN");
        }

        String pin = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(clock);
        student.setVerificationPin(pin);
        student.setVerificationPinExpiry(now.plusMinutes(15));
        LocalDateTime windowStart = student.getPinSendWindowStart();
        if (windowStart == null || !now.isBefore(windowStart.plus(SEND_WINDOW))) {
            student.setPinSendWindowStart(now);
            student.setPinSendCount(0);
        }
        student.setPinSendCount(student.getPinSendCount() + 1);
        student.setPinLastSentAt(now);
        student.setPinFailedAttempts(0);
        studentRepository.save(student);

        logger.info("{}", pinLogMessage(student.getEmail(), pin));

        // Try each configured sender in order (webhook -> Resend -> SMTP) until one succeeds.
        for (EmailSender sender : emailSenders) {
            if (sender.send(student.getEmail(), pin)) {
                break;
            }
        }

        return pin;
    }

    public Student verifyPin(String email, String pin, String deviceId) {
        if (!universityEmailValidator.isValid(email)) {
            throw new InvalidVerificationPinException("Use a valid .edu school email");
        }
        if (pin == null || pin.isBlank()) {
            throw new InvalidVerificationPinException("Verification PIN is required");
        }

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationPinException("Student not found"));

        if (student.getVerificationPin() == null || student.getVerificationPinExpiry() == null) {
            throw new InvalidVerificationPinException("No active verification PIN found. Please request a new one.");
        }

        if (LocalDateTime.now(clock).isAfter(student.getVerificationPinExpiry())) {
            throw new InvalidVerificationPinException("Verification PIN has expired. Please request a new one.");
        }

        if (!pin.trim().equals(student.getVerificationPin())) {
            int attempts = student.getPinFailedAttempts() + 1;
            if (attempts >= MAX_PIN_ATTEMPTS) {
                student.setPinFailedAttempts(0);
                student.setVerificationPin(null);
                student.setVerificationPinExpiry(null);
                studentRepository.save(student);
                throw new InvalidVerificationPinException("Too many incorrect attempts. Please request a new PIN.");
            }
            student.setPinFailedAttempts(attempts);
            studentRepository.save(student);
            throw new InvalidVerificationPinException("Incorrect verification PIN");
        }
        student.setPinFailedAttempts(0);

        student.setVerified(true);
        student.setEmailVerified(true);
        student.setVerificationPin(null);
        student.setVerificationPinExpiry(null);

        if (deviceId != null && !deviceId.isBlank()) {
            student.trustDevice(deviceId);
        }

        return studentRepository.save(student);
    }

    public void resendPin(String email) {
        if (!universityEmailValidator.isValid(email)) {
            throw new InvalidSignupException("Use a valid .edu school email");
        }

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidSignupException("Account not found with this email"));

        PinRequestLimitException limit = sendLimit(student);
        if (limit != null) {
            throw limit;
        }

        generateAndSendPin(student);
    }

    /** Whole seconds from now until the deadline, rounded up so a wait is never reported as shorter than it is. */
    private static long secondsUntil(LocalDateTime now, LocalDateTime deadline) {
        return (Duration.between(now, deadline).toMillis() + 999) / 1000;
    }

    /** Why a new PIN cannot be sent right now, or null when it can. */
    private PinRequestLimitException sendLimit(Student student) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime lastSent = student.getPinLastSentAt();
        if (lastSent != null && now.isBefore(lastSent.plus(resendCooldown))) {
            long seconds = secondsUntil(now, lastSent.plus(resendCooldown));
            return new PinRequestLimitException("Please wait " + seconds + " seconds before requesting a new PIN.",
                    seconds);
        }
        LocalDateTime windowStart = student.getPinSendWindowStart();
        boolean windowOpen = windowStart != null && now.isBefore(windowStart.plus(SEND_WINDOW));
        if (windowOpen && student.getPinSendCount() >= maxSendsPerHour) {
            long seconds = secondsUntil(now, windowStart.plus(SEND_WINDOW));
            long minutes = Math.max(1, (seconds + 59) / 60);
            return new PinRequestLimitException("Too many PIN requests. Please try again in " + minutes + " minutes.",
                    seconds);
        }
        return null;
    }

    /** Sends a PIN unless the cooldown or hourly cap applies; then the current PIN stays valid. */
    public boolean sendPinUnlessLimited(Student student) {
        if (sendLimit(student) != null) {
            return false;
        }
        generateAndSendPin(student);
        return true;
    }

    String pinLogMessage(String email, String pin) {
        String shownPin = logPin ? "  PIN: " + pin + "\n" : "";
        return "\n=======================================================\n" + "  [EMAIL VERIFICATION PIN] Sent to: "
                + email + "\n" + shownPin + "  Expires in: 15 minutes\n"
                + "=======================================================";
    }
}
