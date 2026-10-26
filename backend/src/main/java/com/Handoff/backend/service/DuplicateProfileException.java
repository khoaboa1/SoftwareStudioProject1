package com.Handoff.backend.service;

/**
 * Exception thrown when a user attempts to create a profile when one already exists.
 * Maps to HTTP 409 Conflict status.
 */
public class DuplicateProfileException extends RuntimeException {

  public DuplicateProfileException(String message) {
    super(message);
  }
}
