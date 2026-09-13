package com.Handoff.backend.dto;

public record VerificationRequest(String email, String pin, String deviceId) {
  public VerificationRequest(String email, String pin) {
    this(email, pin, null);
  }
}
