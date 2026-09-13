package com.Handoff.backend.service;

public class InvalidListingException extends RuntimeException {
  public InvalidListingException(String message) {
    super(message);
  }
}
