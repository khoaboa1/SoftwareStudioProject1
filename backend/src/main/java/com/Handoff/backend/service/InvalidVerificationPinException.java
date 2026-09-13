package com.Handoff.backend.service;

public class InvalidVerificationPinException extends RuntimeException {
  public InvalidVerificationPinException(String message) {
    super(message);
  }

  public InvalidVerificationPinException() {
    super("Invalid or expired verification PIN");
  }
}
