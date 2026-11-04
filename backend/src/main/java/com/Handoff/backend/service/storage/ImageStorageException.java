package com.Handoff.backend.service.storage;

public class ImageStorageException extends RuntimeException {
  private final int statusCode;

  public ImageStorageException(int statusCode) {
    super("Photo storage failed with status " + statusCode);
    this.statusCode = statusCode;
  }

  public int getStatusCode() { return statusCode; }
}
