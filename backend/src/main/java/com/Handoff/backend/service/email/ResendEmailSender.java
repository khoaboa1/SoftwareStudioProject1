package com.Handoff.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Resend HTTPS API (used when the webhook is not configured or fails). */
@Component
@Order(2)
public class ResendEmailSender implements EmailSender {

  private static final Logger logger = LoggerFactory.getLogger(ResendEmailSender.class);

  private final String resendApiKey;
  private final RestClient restClient = RestClient.create();

  public ResendEmailSender(@Value("${resend.api.key:}") String resendApiKey) {
    this.resendApiKey = resendApiKey;
  }

  @Override
  public boolean send(String to, String pin) {
    if (resendApiKey == null || resendApiKey.isBlank()) {
      return false;
    }
    try {
      Map<String, Object> payload = Map.of(
          "from", "Handoff <onboarding@resend.dev>",
          "to", List.of(to),
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

      logger.info("[RESEND HTTPS DISPATCH] Successfully sent email via HTTPS to {}: {}", to, responseBody);
      return true;
    } catch (Exception ex) {
      logger.warn("[RESEND DISPATCH WARNING] Could not send via Resend API: {}. PIN is still available in backend console.", ex.getMessage());
      return false;
    }
  }
}
