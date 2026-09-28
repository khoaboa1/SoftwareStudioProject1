package com.Handoff.backend.service;

/**
 * Exception thrown when a user's profile is not found.
 * Maps to HTTP 404 Not Found status.
 */
public class ProfileNotFoundException extends RuntimeException {

  public ProfileNotFoundException(String message) {
    super(message);
  }
}