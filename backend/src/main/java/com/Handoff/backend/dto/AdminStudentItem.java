package com.Handoff.backend.dto;

import com.Handoff.backend.model.Profile;
import java.time.LocalDateTime;

public record AdminStudentItem(Long id, String name, String email, String role, boolean suspended,
        LocalDateTime createdAt) {
    public static AdminStudentItem from(Profile profile) {
        var student = profile.getStudent();
        return new AdminStudentItem(student.getId(), profile.getName(), student.getEmail(), student.getRole().name(),
                student.isSuspended(), student.getCreatedAt());
    }
}
