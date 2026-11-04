package com.Handoff.backend.service;

public class InvalidImageException extends RuntimeException {
  public InvalidImageException() {
    super("Photos must be JPEG, PNG or WebP and at most 5 MB");
  }
}
