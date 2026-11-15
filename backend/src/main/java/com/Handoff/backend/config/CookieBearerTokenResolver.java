package com.Handoff.backend.config;

import com.Handoff.backend.controller.AuthCookies;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Reads the Supabase access token from the HttpOnly sb-access cookie instead of an Authorization header.
 */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {
    /** Auth actions must still work when the browser holds an expired or bad access cookie. */
    static final Set<String> AUTH_ACTIONS = Set.of("/auth/signup", "/auth/login", "/auth/verify-pin",
            "/auth/resend-pin", "/auth/refresh", "/auth/logout");

    @Override
    public String resolve(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (AUTH_ACTIONS.contains(path)) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthCookies.ACCESS.equals(cookie.getName()) && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
