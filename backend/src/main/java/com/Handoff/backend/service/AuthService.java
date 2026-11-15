package com.Handoff.backend.service;

import com.Handoff.backend.dto.LoginResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.auth.EmailNotConfirmedException;
import com.Handoff.backend.service.auth.LoginOutcome;
import com.Handoff.backend.service.auth.PendingLoginStore;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import com.Handoff.backend.service.auth.SupabaseSession;
import com.Handoff.backend.service.auth.VerifiedLogin;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Sign-up and sign-in through Supabase Auth. The frontend only calls our /auth endpoints; this service is the only
 * place that asks Supabase to create users or issue tokens.
 */
@Service
public class AuthService {
    static final String UNVERIFIED_MESSAGE = "Account is unverified. A new verification PIN has been sent to your school email.";
    static final String NEW_DEVICE_MESSAGE = "New device detected. We sent a 6-digit PIN to your school email.";
    static final String LOGIN_EXPIRED_MESSAGE = "Your sign-in expired. Please log in again.";
    private final StudentRepository studentRepository;
    private final VerificationService verificationService;
    private final UniversityEmailValidator universityEmailValidator;
    private final SupabaseAuthClient supabase;
    private final PendingLoginStore pendingLogins;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StudentRepository studentRepository, VerificationService verificationService,
            UniversityEmailValidator universityEmailValidator, SupabaseAuthClient supabase,
            PendingLoginStore pendingLogins, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.verificationService = verificationService;
        this.universityEmailValidator = universityEmailValidator;
        this.supabase = supabase;
        this.pendingLogins = pendingLogins;
        this.passwordEncoder = passwordEncoder;
    }

    public Student signup(String studentName, String email, String password) {
        if (studentName == null || studentName.isBlank()) {
            throw new InvalidSignupException("Full name is required");
        }
        if (!universityEmailValidator.isValid(email)) {
            throw new InvalidSignupException("Use a valid .edu school email");
        }
        if (password == null || password.length() < 8) {
            throw new InvalidSignupException("Password must be at least 8 characters");
        }
        // Supabase hides whether an email is taken, so check our own record first.
        Optional<Student> existing = studentRepository.findByEmail(email);
        if (existing.isPresent() && existing.get().isVerified()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }
        // Supabase creates the user, stores the password hash and emails the 6-digit
        // code.
        UUID supabaseUserId = supabase.signUp(email, password);
        Student student = existing.orElseGet(() -> new Student(studentName, email, null, null, null));
        student.setStudentName(studentName);
        student.setSupabaseUserId(supabaseUserId);
        student.setCreatedAt(LocalDateTime.now());
        return studentRepository.save(student);
    }

    public LoginOutcome login(String email, String password, String deviceId) {
        Student student = studentRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        SupabaseSession session;
        try {
            session = supabase.passwordGrant(email, password);
        } catch (EmailNotConfirmedException ex) {
            supabase.resendSignupOtp(email);
            return new LoginOutcome(LoginResponse.requiresPin(email, UNVERIFIED_MESSAGE), null);
        } catch (InvalidCredentialsException ex) {
            session = migrateLegacyAccount(student, email, password).orElseThrow(() -> ex);
        }
        student.setSupabaseUserId(session.userId());
        if (!student.isVerified()) {
            // Supabase already confirmed this email, for example through its own email
            // link.
            student.setVerified(true);
            student.setEmailVerified(true);
        }
        if (deviceId == null || deviceId.isBlank() || !student.isDeviceTrusted(deviceId)) {
            Student saved = studentRepository.save(student);
            pendingLogins.put(email, deviceId, session);
            verificationService.generateAndSendPin(saved);
            return new LoginOutcome(LoginResponse.requiresPin(email, NEW_DEVICE_MESSAGE), null);
        }
        Student saved = studentRepository.save(student);
        return new LoginOutcome(LoginResponse.success(saved), session);
    }

    /**
     * Accounts created before SSP1-116 only have a bcrypt hash in our table and no Supabase user. On their first login
     * with the right password, create the confirmed Supabase user and drop the old hash, so the student keeps their
     * profile and listings.
     */
    private Optional<SupabaseSession> migrateLegacyAccount(Student student, String email, String password) {
        if (!student.isVerified() || student.getSupabaseUserId() != null || student.getPasswordHash() == null
                || password == null || !passwordEncoder.matches(password, student.getPasswordHash())) {
            return Optional.empty();
        }
        try {
            supabase.adminCreateConfirmedUser(email, password);
            SupabaseSession session = supabase.passwordGrant(email, password);
            student.setPasswordHash(null);
            return Optional.of(session);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public VerifiedLogin verifyPin(String email, String pin, String deviceId) {
        if (!universityEmailValidator.isValid(email)) {
            throw new InvalidVerificationPinException("Use a valid .edu school email");
        }
        if (pin == null || pin.isBlank()) {
            throw new InvalidVerificationPinException("Verification PIN is required");
        }
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationPinException("Student not found"));
        if (!student.isVerified()) {
            // Sign-up: the code was emailed by Supabase, so Supabase checks it.
            SupabaseSession session = supabase.verifySignupOtp(email, pin.trim());
            student.setSupabaseUserId(session.userId());
            student.setVerified(true);
            student.setEmailVerified(true);
            if (deviceId != null && !deviceId.isBlank()) {
                student.trustDevice(deviceId);
            }
            return new VerifiedLogin(studentRepository.save(student), session);
        }
        // New device: our own PIN must pass before the held Supabase session is
        // released.
        SupabaseSession pending = pendingLogins.get(email, deviceId)
                .orElseThrow(() -> new InvalidCredentialsException(LOGIN_EXPIRED_MESSAGE));
        Student verified = verificationService.verifyPin(email, pin, deviceId);
        pendingLogins.remove(email, deviceId);
        return new VerifiedLogin(verified, pending);
    }

    public void resendPin(String email) {
        if (!universityEmailValidator.isValid(email)) {
            throw new InvalidSignupException("Use a valid .edu school email");
        }
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidSignupException("Account not found with this email"));
        if (student.isVerified()) {
            verificationService.resendPin(email);
        } else {
            supabase.resendSignupOtp(email);
        }
    }

    public SupabaseSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException("Please log in again.");
        }
        return supabase.refreshGrant(refreshToken);
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            supabase.logout(accessToken);
        } catch (RuntimeException ex) {
            // Best effort: the auth cookies are cleared either way.
        }
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    public Optional<Student> findBySupabaseUserId(UUID supabaseUserId) {
        return studentRepository.findBySupabaseUserId(supabaseUserId);
    }
}
