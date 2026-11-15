package com.Handoff.backend.service.auth;

public class SupabaseAuthException extends RuntimeException {
  private final int status;
  private final String code;

  public SupabaseAuthException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code == null ? "" : code;
  }

  public int getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }
}
