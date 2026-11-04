package com.Handoff.backend.service.storage;

public class ImageStorageNotConfiguredException extends RuntimeException {
  public ImageStorageNotConfiguredException() {
    super("Photo uploads are not configured");
  }
}
