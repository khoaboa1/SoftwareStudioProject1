package com.Handoff.backend.service.storage;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SupabaseImageStorageTest {
  @Test
  void store_postsBytesToBucketPath_withAuthHeaders() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<String> request = new AtomicReference<>();
    server.createContext("/", exchange -> {
      request.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " "
          + exchange.getRequestHeaders().getFirst("Authorization") + " "
          + exchange.getRequestHeaders().getFirst("apikey") + " "
          + exchange.getRequestHeaders().getFirst("Content-Type") + " "
          + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
    });
    server.start();
    try {
      String base = "http://127.0.0.1:" + server.getAddress().getPort();
      SupabaseImageStorage storage = new SupabaseImageStorage(base, "secret", "listing-images");
      assertThat(storage.store("listings/7/x.jpg", "photo".getBytes(StandardCharsets.UTF_8), "image/jpeg"))
          .isEqualTo(base + "/storage/v1/object/public/listing-images/listings/7/x.jpg");
      assertThat(request.get()).isEqualTo("POST /storage/v1/object/listing-images/listings/7/x.jpg Bearer secret secret image/jpeg photo");
    } finally { server.stop(0); }
  }

  @Test
  void store_throwsImageStorageException_onNon2xx() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> { exchange.sendResponseHeaders(400, -1); exchange.close(); });
    server.start();
    try {
      SupabaseImageStorage storage = new SupabaseImageStorage("http://127.0.0.1:" + server.getAddress().getPort(), "secret", "listing-images");
      assertThatThrownBy(() -> storage.store("listings/7/x.jpg", new byte[]{1}, "image/jpeg"))
          .isInstanceOf(ImageStorageException.class);
    } finally { server.stop(0); }
  }

  @Test
  void isConfigured_falseWhenUrlOrKeyBlank() {
    assertThat(new SupabaseImageStorage("", "key", "listing-images").isConfigured()).isFalse();
    assertThat(new SupabaseImageStorage("https://example.com", " ", "listing-images").isConfigured()).isFalse();
  }

  @Test
  void store_throwsNotConfigured_whenBlank() {
    assertThatThrownBy(() -> new SupabaseImageStorage("", "", "listing-images")
        .store("x", new byte[]{1}, "image/jpeg"))
        .isInstanceOf(ImageStorageNotConfiguredException.class);
  }
}
