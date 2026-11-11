package com.Handoff.backend.service;

public class OwnListingConversationException extends RuntimeException {
  public OwnListingConversationException() {
    super("You cannot message yourself about your own listing");
  }
}
