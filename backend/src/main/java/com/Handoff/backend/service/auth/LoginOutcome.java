package com.Handoff.backend.service.auth;

import com.Handoff.backend.dto.LoginResponse;

/**
 * Result of a login attempt.
 * <p>
 * {@code session} is null when a PIN is still required.
 */
public record LoginOutcome(LoginResponse response, SupabaseSession session) {
}
