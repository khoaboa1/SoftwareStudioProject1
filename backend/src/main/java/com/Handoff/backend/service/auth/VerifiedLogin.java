package com.Handoff.backend.service.auth;

import com.Handoff.backend.model.Student;

public record VerifiedLogin(Student student, SupabaseSession session) {
}
