package com.Handoff.backend.service;

/** A PIN was requested too soon or too often. Maps to 429 with a Retry-After header. */
public class PinRequestLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public PinRequestLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
