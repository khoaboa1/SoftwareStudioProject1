package com.Handoff.backend.service.email;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookEmailSenderTest {

  private HttpServer server;
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicReference<String> contentType = new AtomicReference<>();
  private final AtomicReference<String> method = new AtomicReference<>();

  private String startWebhook(int status) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/mail", exchange -> {
      try {
        method.set(exchange.getRequestMethod());
        contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        exchange.sendResponseHeaders(status, -1);
      } finally {
        exchange.close();
      }
    });
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/mail";
  }

  @AfterEach
  void stopWebhook() {
    if (server != null) server.stop(0);
  }

  @Test
  void sendsTheExistingWebhookContract() throws IOException {
    WebhookEmailSender sender = new WebhookEmailSender(startWebhook(200));

    assertThat(sender.send("bob@tulane.edu", "123456")).isTrue();

    JsonNode payload = JsonMapper.builder().build().readTree(requestBody.get());
    assertThat(method.get()).isEqualTo("POST");
    assertThat(contentType.get()).isEqualTo("application/json");
    assertThat(payload.size()).isEqualTo(3);
    assertThat(payload.get("to").asText()).isEqualTo("bob@tulane.edu");
    assertThat(payload.get("subject").asText()).isEqualTo("Your Handoff Verification PIN: 123456");
    assertThat(payload.get("body").asText()).isEqualTo(
        "Welcome to Handoff!\n\nYour 6-digit verification PIN is: 123456\n\n" +
        "This PIN will expire in 15 minutes.\n\nBest,\nThe Handoff Team");
  }

  @Test
  void escapesRecipientTextWithoutChangingItsValue() throws IOException {
    WebhookEmailSender sender = new WebhookEmailSender(startWebhook(200));
    String recipient = "bob\"quoted\\name@tulane.edu";

    assertThat(sender.send(recipient, "123456")).isTrue();

    JsonNode payload = JsonMapper.builder().build().readTree(requestBody.get());
    assertThat(payload.size()).isEqualTo(3);
    assertThat(payload.get("to").asText()).isEqualTo(recipient);
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 201, 204, 299, 400, 503})
  void preservesSuccessAndFallbackStatusHandling(int status) throws IOException {
    WebhookEmailSender sender = new WebhookEmailSender(startWebhook(status));

    assertThat(sender.send("bob@tulane.edu", "123456"))
        .isEqualTo(status >= 200 && status < 300);
  }
}
