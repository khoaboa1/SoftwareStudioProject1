package com.Handoff.backend.service;

/**
 * Exception thrown when a conversation does not exist.
 * Maps to HTTP 404 Not Found status.
 */
public class ConversationNotFoundException extends RuntimeException {

  public ConversationNotFoundException() {
    super("Conversation not found");
  }
}
