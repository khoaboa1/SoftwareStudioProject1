package com.Handoff.backend.service;

/** Raised when public profile-search query parameters are invalid. */
public class InvalidProfileSearchException extends RuntimeException {

  public InvalidProfileSearchException(String message) {
    super(message);
  }
}
