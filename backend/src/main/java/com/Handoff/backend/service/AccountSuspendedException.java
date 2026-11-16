package com.Handoff.backend.service;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException() {
        super("This account has been suspended");
    }
}
