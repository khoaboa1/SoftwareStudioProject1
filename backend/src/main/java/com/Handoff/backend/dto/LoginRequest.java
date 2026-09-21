package com.Handoff.backend.dto;

public record LoginRequest(String email, String password, String deviceId) {
  public LoginRequest(String email, String password) {
    this(email, password, null);
  }
}
