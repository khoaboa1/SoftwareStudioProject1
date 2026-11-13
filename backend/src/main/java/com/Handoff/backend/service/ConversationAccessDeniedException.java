package com.Handoff.backend.service;

/**
 * Exception thrown when a student tries to access a conversation they are not a participant of.
 * Maps to HTTP 403 Forbidden status.
 */
public class ConversationAccessDeniedException extends RuntimeException {

  public ConversationAccessDeniedException() {
    super("You do not have access to this conversation");
  }
}
