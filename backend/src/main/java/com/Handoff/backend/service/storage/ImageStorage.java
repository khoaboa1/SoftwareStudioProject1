package com.Handoff.backend.service.storage;

public interface ImageStorage {
  boolean isConfigured();
  String store(String key, byte[] bytes, String contentType);
}
