package com.Handoff.backend.service;

public class ForbiddenListingActionException extends RuntimeException {
  public ForbiddenListingActionException() {
    super("You do not have permission to modify this listing");
  }
}
