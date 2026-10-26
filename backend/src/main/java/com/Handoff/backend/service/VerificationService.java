package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class VerificationService {

  private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  private final StudentRepository studentRepository;
  private final List<EmailSender> emailSenders;
  private final SecureRandom secureRandom = new SecureRandom();

  /** Spring injects every EmailSender bean, sorted by {@code @Order}. */
  public VerificationService(StudentRepository studentRepository, List<EmailSender> emailSenders) {
    this.studentRepository = studentRepository;
    this.emailSenders = emailSenders;
  }

  public String generateAndSendPin(Student student) {
    if (student.getEmail() == null || !EDU_EMAIL_PATTERN.matcher(student.getEmail()).matches()) {
      throw new InvalidSignupException("Only .edu school emails can receive a verification PIN");
    }

    String pin = String.format("%06d", secureRandom.nextInt(1_000_000));
    student.setVerificationPin(pin);
    student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(15));
    studentRepository.save(student);

    logger.info("\n=======================================================\n" +
                "  [EMAIL VERIFICATION PIN] Sent to: {}\n" +
                "  PIN: {}\n" +
                "  Expires in: 15 minutes\n" +
                "=======================================================", student.getEmail(), pin);

    // Try each configured sender in order (webhook -> Resend -> SMTP) until one succeeds.
    for (EmailSender sender : emailSenders) {
      if (sender.send(student.getEmail(), pin)) {
        break;
      }
    }

    return pin;
  }

  public Student verifyPin(String email, String pin, String deviceId) {
    if (email == null || !EDU_EMAIL_PATTERN.matcher(email).matches()) {
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

    if (LocalDateTime.now().isAfter(student.getVerificationPinExpiry())) {
      throw new InvalidVerificationPinException("Verification PIN has expired. Please request a new one.");
    }

    if (!pin.trim().equals(student.getVerificationPin())) {
      throw new InvalidVerificationPinException("Incorrect verification PIN");
    }

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
    if (email == null || !EDU_EMAIL_PATTERN.matcher(email).matches()) {
      throw new InvalidSignupException("Use a valid .edu school email");
    }

    Student student = studentRepository.findByEmail(email)
        .orElseThrow(() -> new InvalidSignupException("Account not found with this email"));

    generateAndSendPin(student);
  }
}
