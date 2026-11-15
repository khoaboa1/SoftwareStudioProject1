package com.Handoff.backend.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import java.time.Instant;

/**
 * Accepts the fake tokens issued by FakeSupabaseAuthClient instead of real
 * Supabase JWTs.
 */
@Configuration
public class FakeJwtDecoderConfig {
  @Bean
  @Primary
  public JwtDecoder fakeJwtDecoder() {
    return token -> {
      if (token == null || !token.startsWith(FakeSupabaseAuthClient.ACCESS_PREFIX)) {
        throw new BadJwtException("Invalid test token");
      }
      Instant now = Instant.now();
      return Jwt.withTokenValue(token)
          .header("alg", "none")
          .subject(token.substring(FakeSupabaseAuthClient.ACCESS_PREFIX.length()))
          .issuedAt(now)
          .expiresAt(now.plusSeconds(3600))
          .build();
    };
  }
}
