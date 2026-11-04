package com.Handoff.backend.service.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupabaseImageStorage implements ImageStorage {
  private final String baseUrl;
  private final String serviceKey;
  private final String bucket;
  private final HttpClient client;

  @Autowired
  public SupabaseImageStorage(@Value("${supabase.url:}") String baseUrl,
                              @Value("${supabase.service-key:}") String serviceKey,
                              @Value("${supabase.storage.bucket:listing-images}") String bucket) {
    this(baseUrl, serviceKey, bucket, HttpClient.newHttpClient());
  }

  SupabaseImageStorage(String baseUrl, String serviceKey, String bucket, HttpClient client) {
    this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    this.serviceKey = serviceKey;
    this.bucket = bucket;
    this.client = client;
  }

  @Override
  public boolean isConfigured() {
    return !baseUrl.isBlank() && serviceKey != null && !serviceKey.isBlank();
  }

  @Override
  public String store(String key, byte[] bytes, String contentType) {
    if (!isConfigured()) throw new ImageStorageNotConfiguredException();
    String path = "/storage/v1/object/" + bucket + "/" + key;
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Authorization", "Bearer " + serviceKey)
        .header("apikey", serviceKey)
        .header("Content-Type", contentType)
        .header("x-upsert", "false")
        .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
    try {
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ImageStorageException(response.statusCode());
      }
    } catch (IOException ex) {
      throw new ImageStorageException(-1);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ImageStorageException(-1);
    }
    return baseUrl + "/storage/v1/object/public/" + bucket + "/" + key;
  }
}
