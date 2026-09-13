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

import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
public class VerificationService {

  private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  private final StudentRepository studentRepository;
  private final Optional<JavaMailSender> mailSender;
  private final RestClient restClient;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${spring.mail.username:}")
  private String senderEmail;

  @Value("${resend.api.key:}")
  private String resendApiKey;

  @Value("${mail.webhook.url:}")
  private String mailWebhookUrl;

  public VerificationService(StudentRepository studentRepository,
                             @Autowired(required = false) JavaMailSender mailSender) {
    this.studentRepository = studentRepository;
    this.mailSender = Optional.ofNullable(mailSender);
    this.restClient = RestClient.create();
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

    // 1. Attempt HTTPS dispatch via Google Apps Script Webhook Relay (Method 1 - Port 443, works on campus Wi-Fi)
    boolean emailSent = false;
    if (mailWebhookUrl != null && !mailWebhookUrl.isBlank()) {
      try {
        String jsonPayload = String.format(
            "{\"to\":\"%s\",\"subject\":\"Your Handoff Verification PIN: %s\",\"body\":\"Welcome to Handoff!\\n\\nYour 6-digit verification PIN is: %s\\n\\nThis PIN will expire in 15 minutes.\\n\\nBest,\\nThe Handoff Team\"}",
            student.getEmail(), pin, pin
        );

        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(mailWebhookUrl.trim()))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.info("[WEBHOOK HTTPS DISPATCH] Successfully sent verification email to {}: status {}", student.getEmail(), response.statusCode());
          emailSent = true;
        } else {
          logger.warn("[WEBHOOK DISPATCH WARNING] Webhook returned status {}: {}. PIN is still available in backend console.", response.statusCode(), response.body());
        }
      } catch (Exception ex) {
        logger.warn("[WEBHOOK DISPATCH WARNING] Could not send via Webhook: {}. PIN is still available in backend console.", ex.getMessage());
      }
    }

    // 2. Attempt HTTPS dispatch via Resend API (if configured and webhook was not used)
    if (!emailSent && resendApiKey != null && !resendApiKey.isBlank()) {
      try {
        Map<String, Object> payload = Map.of(
            "from", "Handoff <onboarding@resend.dev>",
            "to", List.of(student.getEmail()),
            "subject", "Your Handoff Verification PIN: " + pin,
            "html", "<div style='font-family: sans-serif; padding: 20px; color: #333;'>" +
                    "<h2>Welcome to Handoff!</h2>" +
                    "<p>Your 6-digit verification PIN for your university account is:</p>" +
                    "<div style='font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #c2410c; margin: 16px 0;'>" + pin + "</div>" +
                    "<p>This PIN will expire in 15 minutes.</p>" +
                    "<p style='color: #777; font-size: 12px;'>If you did not request this, please ignore this email.</p>" +
                    "</div>"
        );

        String responseBody = restClient.post()
            .uri("https://api.resend.com/emails")
            .header("Authorization", "Bearer " + resendApiKey.trim())
            .header("Content-Type", "application/json")
            .body(payload)
            .retrieve()
            .body(String.class);

        logger.info("[RESEND HTTPS DISPATCH] Successfully sent email via HTTPS to {}: {}", student.getEmail(), responseBody);
        emailSent = true;
      } catch (Exception ex) {
        logger.warn("[RESEND DISPATCH WARNING] Could not send via Resend API: {}. PIN is still available in backend console.", ex.getMessage());
      }
    }

    // 3. Fallback to JavaMailSender if HTTPS methods were not used
    if (!emailSent) {
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
              "PIN is still available in backend console.", ex.getMessage());
        }
      });
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
