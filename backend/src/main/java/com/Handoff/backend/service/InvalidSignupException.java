package com.Handoff.backend.service;

public class InvalidSignupException extends RuntimeException {
  public InvalidSignupException(String message) {
    super(message);
  }
}
