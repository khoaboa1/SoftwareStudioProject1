package com.Handoff.backend.service;

public class ProfileAccessDeniedException extends RuntimeException {
  public ProfileAccessDeniedException(String message) {
    super(message);
  }
}
