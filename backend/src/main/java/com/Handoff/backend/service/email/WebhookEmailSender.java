package com.Handoff.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Google Apps Script webhook relay over HTTPS (port 443, works on campus Wi-Fi). */
@Component
@Order(1)
public class WebhookEmailSender implements EmailSender {

  private static final Logger logger = LoggerFactory.getLogger(WebhookEmailSender.class);

  private final String mailWebhookUrl;

  public WebhookEmailSender(@Value("${mail.webhook.url:}") String mailWebhookUrl) {
    this.mailWebhookUrl = mailWebhookUrl;
  }

  @Override
  public boolean send(String to, String pin) {
    if (mailWebhookUrl == null || mailWebhookUrl.isBlank()) {
      return false;
    }
    try {
      String jsonPayload = String.format(
          "{\"to\":\"%s\",\"subject\":\"Your Handoff Verification PIN: %s\",\"body\":\"Welcome to Handoff!\\n\\nYour 6-digit verification PIN is: %s\\n\\nThis PIN will expire in 15 minutes.\\n\\nBest,\\nThe Handoff Team\"}",
          to, pin, pin
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
        logger.info("[WEBHOOK HTTPS DISPATCH] Successfully sent verification email to {}: status {}", to, response.statusCode());
        return true;
      }
      logger.warn("[WEBHOOK DISPATCH WARNING] Webhook returned status {}: {}. PIN is still available in backend console.", response.statusCode(), response.body());
    } catch (Exception ex) {
      logger.warn("[WEBHOOK DISPATCH WARNING] Could not send via Webhook: {}. PIN is still available in backend console.", ex.getMessage());
    }
    return false;
  }
}
