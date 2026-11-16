package com.Handoff.backend.service;

public class AdminAccessDeniedException extends RuntimeException {
    public AdminAccessDeniedException() {
        super("Admin access required");
    }
}
