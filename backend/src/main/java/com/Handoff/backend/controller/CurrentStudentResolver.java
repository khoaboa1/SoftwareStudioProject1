package com.Handoff.backend.controller;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.model.Role;
import com.Handoff.backend.service.AdminAccessDeniedException;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.NotAuthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the logged-in student from the verified Supabase JWT (read from the sb-access cookie by Spring Security), so
 * controllers only handle HTTP routing.
 */
@Component
public class CurrentStudentResolver {

    private final AuthService authService;

    public CurrentStudentResolver(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Returns the token's student, if the token's subject still matches a real one.
     */
    public Optional<Student> find(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return Optional.empty();
        }
        String subject = jwtAuthentication.getToken().getSubject();
        if (subject == null) {
            return Optional.empty();
        }
        try {
            // A suspended student is treated as logged out, even with a still-valid token.
            return authService.findBySupabaseUserId(UUID.fromString(subject))
                    .filter(student -> !student.isSuspended());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns the session's student or throws NotAuthenticatedException (mapped to 401 by ApiExceptionHandler) with the
     * given message.
     */
    public Student require(HttpServletRequest request, String errorMessage) {
        return find(request).orElseThrow(() -> new NotAuthenticatedException(errorMessage));
    }

    /**
     * Returns the logged-in student if they are an admin. The role is read from the database on every request, so a
     * demotion takes effect immediately. Throws NotAuthenticatedException (401) or AdminAccessDeniedException (403).
     */
    public Student requireAdmin(HttpServletRequest request) {
        Student student = require(request, "You must be logged in");
        if (student.getRole() != Role.ADMIN) {
            throw new AdminAccessDeniedException();
        }
        return student;
    }
}
