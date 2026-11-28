package com.Handoff.backend.dto;

import com.Handoff.backend.model.Profile;

import java.time.LocalDateTime;

/** Read-only profile another student at the same school may see. Leaves out the email. */
public record PublicProfileResponse(Long id, Long studentId, String name, String major, String bio, String schoolDomain,
        LocalDateTime createdAt, boolean verified) {
    public static PublicProfileResponse from(Profile profile) {
        return new PublicProfileResponse(profile.getId(), profile.getStudentId(), profile.getName(), profile.getMajor(),
                profile.getBio(), profile.getSchoolDomain(), profile.getCreatedAt(), profile.getStudent().isVerified());
    }
}
