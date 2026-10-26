package com.Handoff.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    String name, 
    String major, 
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    String bio
) {
}
