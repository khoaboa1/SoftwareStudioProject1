package com.Handoff.backend.service.auth;

import java.util.UUID;

public record SupabaseAdminUser(UUID id, boolean emailConfirmed) {
}
