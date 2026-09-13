package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class VerificationService {

  private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  private final StudentRepository studentRepository;
  private final Optional<JavaMailSender> mailSender;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${spring.mail.username:}")
  private String senderEmail;

  public VerificationService(StudentRepository studentRepository,
                             @Autowired(required = false) JavaMailSender mailSender) {
    this.studentRepository = studentRepository;
    this.mailSender = Optional.ofNullable(mailSender);
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

    // Attempt to dispatch real email if mailSender is available
    mailSender.ifPresent(sender -> {
      try {
        SimpleMailMessage message = new SimpleMailMessage();
        if (senderEmail != null && !senderEmail.isBlank()) {
          message.setFrom(senderEmail);
        }
        message.setTo(student.getEmail());
        message.setSubject("Your Handoff Verification PIN: " + pin);
        message.setText("Welcome to Handoff!\n\n" +
            "Your 6-digit verification PIN is: " + pin + "\n\n" +
            "This PIN will expire in 15 minutes.\n\n" +
            "Best,\n" +
            "The Handoff Team");

        sender.send(message);
        logger.info("[EMAIL DISPATCH] Successfully sent email to {}", student.getEmail());
      } catch (Exception ex) {
        logger.warn("[EMAIL DISPATCH WARNING] Could not send email via SMTP: {}. " +
            "Make sure your spring.mail.password App Password is set in application.properties. " +
            "PIN is still available in backend console.", ex.getMessage());
      }
    });

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
