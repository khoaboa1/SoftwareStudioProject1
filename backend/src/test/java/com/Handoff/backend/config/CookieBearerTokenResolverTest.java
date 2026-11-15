package com.Handoff.backend.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CookieBearerTokenResolverTest {

  private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

  private MockHttpServletRequest request(String uri, Cookie... cookies) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    if (cookies.length > 0) {
      request.setCookies(cookies);
    }
    return request;
  }

  @Test
  void resolve_returnsAccessCookieValue() {
    assertThat(resolver.resolve(request("/auth/me", new Cookie("sb-access", "token-1"))))
        .isEqualTo("token-1");
  }

  @Test
  void resolve_usesAccessCookieForApiRoutes() {
    assertThat(resolver.resolve(request("/api/conversations", new Cookie("sb-access", "token-1"))))
        .isEqualTo("token-1");
  }

  @Test
  void resolve_returnsNull_withoutCookies() {
    assertThat(resolver.resolve(request("/auth/me"))).isNull();
  }

  @Test
  void resolve_ignoresOtherCookies() {
    assertThat(resolver.resolve(request("/auth/me", new Cookie("JSESSIONID", "abc")))).isNull();
  }

  @Test
  void resolve_ignoresBlankAccessCookie() {
    assertThat(resolver.resolve(request("/auth/me", new Cookie("sb-access", "")))).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/auth/signup", "/auth/login", "/auth/verify-pin", "/auth/resend-pin",
      "/auth/refresh", "/auth/logout"})
  void resolve_skipsAuthActions_soAStaleCookieCannotBlockThem(String uri) {
    assertThat(resolver.resolve(request(uri, new Cookie("sb-access", "expired-token")))).isNull();
  }
}
