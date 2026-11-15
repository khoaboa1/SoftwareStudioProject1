package com.Handoff.backend.service.auth;

import java.util.UUID;

/** The only way the backend talks to Supabase Auth. The frontend never calls Supabase directly. */
public interface SupabaseAuthClient {
    boolean isConfigured();

    UUID signUp(String email, String password);

    SupabaseSession passwordGrant(String email, String password);

    SupabaseSession refreshGrant(String refreshToken);

    SupabaseSession verifySignupOtp(String email, String token);

    void resendSignupOtp(String email);

    void logout(String accessToken);

    UUID adminCreateConfirmedUser(String email, String password);
}
