package com.Handoff.backend.service.auth;

public class EmailNotConfirmedException extends RuntimeException {
    public EmailNotConfirmedException() {
        super("Email not confirmed");
    }
}
