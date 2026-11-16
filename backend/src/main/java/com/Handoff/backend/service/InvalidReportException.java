package com.Handoff.backend.service;

public class InvalidReportException extends RuntimeException {
    public InvalidReportException(String message) {
        super(message);
    }
}
