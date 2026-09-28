package com.Handoff.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new student profile.
 * Only accepts name, major, and bio. Any manually injected user IDs or domains
 * are intentionally ignored by the backend.
 */
public record CreateProfileRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Major is required")
    @Size(max = 255, message = "Major must not exceed 255 characters")
    String major,

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    String bio
) {
}
