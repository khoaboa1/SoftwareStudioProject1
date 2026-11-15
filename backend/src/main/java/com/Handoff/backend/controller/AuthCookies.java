package com.Handoff.backend.controller;

import com.Handoff.backend.service.auth.SupabaseSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Writes the Supabase tokens as HttpOnly cookies, so JavaScript in the browser can never read them. */
@Component
public class AuthCookies {
  public static final String ACCESS = "sb-access";
  public static final String REFRESH = "sb-refresh";
  static final Duration REFRESH_MAX_AGE = Duration.ofDays(30);

  private final boolean secure;

  public AuthCookies(@Value("${auth.cookie.secure:false}") boolean secure) {
    this.secure = secure;
  }

  public void write(HttpServletResponse response, SupabaseSession session) {
    response.addHeader(HttpHeaders.SET_COOKIE,
        cookie(ACCESS, session.accessToken(), "/", Duration.ofSeconds(session.expiresIn())).toString());
    response.addHeader(HttpHeaders.SET_COOKIE,
        cookie(REFRESH, session.refreshToken(), "/auth", REFRESH_MAX_AGE).toString());
  }

  public void clear(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS, "", "/", Duration.ZERO).toString());
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH, "", "/auth", Duration.ZERO).toString());
  }

  private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path(path)
        .maxAge(maxAge)
        .build();
  }
}
