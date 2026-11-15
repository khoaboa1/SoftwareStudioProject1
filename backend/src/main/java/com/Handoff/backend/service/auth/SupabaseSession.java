package com.Handoff.backend.service.auth;

import java.util.UUID;

public record SupabaseSession(String accessToken, String refreshToken, long expiresIn, UUID userId) {
}
