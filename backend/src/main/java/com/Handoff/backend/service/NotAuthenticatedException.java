package com.Handoff.backend.service;

public class NotAuthenticatedException extends RuntimeException {
  public NotAuthenticatedException() {
    super("You must be logged in to post a listing");
  }

  public NotAuthenticatedException(String message) {
    super(message);
  }
}
