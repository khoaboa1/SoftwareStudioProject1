package com.Handoff.backend.service;

public class OwnListingReportException extends RuntimeException {
    public OwnListingReportException() {
        super("You cannot report your own listing");
    }
}
