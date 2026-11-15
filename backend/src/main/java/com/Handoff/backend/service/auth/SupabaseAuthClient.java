package com.Handoff.backend.service.auth;

import java.util.Optional;
import java.util.UUID;

/** The only way the backend talks to Supabase Auth. The frontend never calls Supabase directly. */
public interface SupabaseAuthClient {
    boolean isConfigured();

    SupabaseSession passwordGrant(String email, String password);

    SupabaseSession refreshGrant(String refreshToken);

    void logout(String accessToken);

    UUID adminCreateConfirmedUser(String email, String password);

    UUID adminCreateUnconfirmedUser(String email, String password);

    /** Changes only the fields that are not null. */
    void adminUpdateUser(UUID userId, String password, Boolean emailConfirmed);

    Optional<SupabaseAdminUser> adminFindUserByEmail(String email);
}
