package com.Handoff.backend.controller;

import com.Handoff.backend.service.auth.SupabaseSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookiesTest {

  private static final SupabaseSession SESSION = new SupabaseSession(
      "access-token", "refresh-token", 3600, UUID.fromString("11111111-1111-1111-1111-111111111111"));

  @Test
  void write_setsHttpOnlyLaxAccessAndRefreshCookies() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new AuthCookies(false).write(response, SESSION);
    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(headers).hasSize(2);
    assertThat(headers.get(0)).startsWith("sb-access=access-token;").contains("Path=/;")
        .contains("Max-Age=3600").contains("HttpOnly").contains("SameSite=Lax").doesNotContain("Secure");
    assertThat(headers.get(1)).startsWith("sb-refresh=refresh-token;").contains("Path=/auth")
        .contains("Max-Age=2592000").contains("HttpOnly").contains("SameSite=Lax");
  }

  @Test
  void write_marksCookiesSecureWhenConfigured() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new AuthCookies(true).write(response, SESSION);
    assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).allMatch(header -> header.contains("Secure"));
  }

  @Test
  void clear_expiresBothCookies() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new AuthCookies(false).clear(response);
    List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(headers).hasSize(2);
    assertThat(headers.get(0)).startsWith("sb-access=;").contains("Max-Age=0").contains("Path=/;");
    assertThat(headers.get(1)).startsWith("sb-refresh=;").contains("Max-Age=0").contains("Path=/auth");
  }
}
