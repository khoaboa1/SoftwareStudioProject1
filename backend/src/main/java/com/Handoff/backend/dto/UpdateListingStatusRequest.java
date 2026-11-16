package com.Handoff.backend.dto;

/** Body of {@code PATCH /listings/{id}/status}: ACTIVE, PENDING or SOLD. */
public record UpdateListingStatusRequest(String status) {
}
