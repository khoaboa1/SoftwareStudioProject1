package com.Handoff.backend.service;

/** Thrown when a message's content is missing or too long. Maps to HTTP 400. */
public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String message) {
        super(message);
    }
}
