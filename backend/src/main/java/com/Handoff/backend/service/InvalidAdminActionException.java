package com.Handoff.backend.service;

public class InvalidAdminActionException extends RuntimeException {
    public InvalidAdminActionException(String message) {
        super(message);
    }
}
