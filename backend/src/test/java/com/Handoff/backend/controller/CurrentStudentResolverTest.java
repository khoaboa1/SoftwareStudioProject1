package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.model.Role;
import com.Handoff.backend.service.AdminAccessDeniedException;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.NotAuthenticatedException;
import com.Handoff.backend.service.ProfileNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentStudentResolverTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Mock
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void require_returnsStudentForTokenSubject() {
        Student bob = new Student("Bob", "bob@tulane.edu", null, null, null);
        authenticateAs(USER_ID.toString());
        when(authService.findBySupabaseUserId(USER_ID)).thenReturn(Optional.of(bob));
        CurrentStudentResolver resolver = new CurrentStudentResolver(authService);
        assertThat(resolver.require(new MockHttpServletRequest(), "Log in first")).isSameAs(bob);
    }

    @Test
    void require_throwsWithGivenMessage_whenNotAuthenticated() {
        CurrentStudentResolver resolver = new CurrentStudentResolver(authService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThatThrownBy(() -> resolver.require(request, "Log in first"))
                .isInstanceOf(NotAuthenticatedException.class).hasMessage("Log in first");
        // Must not create a session as a side effect.
        assertThat(request.getSession(false)).isNull();
        verifyNoInteractions(authService);
    }

    @Test
    void require_throws_whenTokenStudentNoLongerExists() {
        authenticateAs(USER_ID.toString());
        when(authService.findBySupabaseUserId(USER_ID)).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        CurrentStudentResolver resolver = new CurrentStudentResolver(authService);
        assertThatThrownBy(() -> resolver.require(request, "Log in first"))
                .isInstanceOf(NotAuthenticatedException.class);
    }

    @Test
    void find_returnsEmpty_whenSubjectIsNotUuid() {
        authenticateAs("not-a-uuid");
        assertThat(new CurrentStudentResolver(authService).find(new MockHttpServletRequest())).isEmpty();
        verifyNoInteractions(authService);
    }

    @Test
    void find_returnsEmpty_forNonJwtAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));
        assertThat(new CurrentStudentResolver(authService).find(new MockHttpServletRequest())).isEmpty();
        verifyNoInteractions(authService);
    }

    @Test
    void apiExceptionHandler_mapsSharedExceptionsToStatusAndMessage() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ResponseEntity<ErrorResponse> unauthorized = handler
                .handleNotAuthenticated(new NotAuthenticatedException("Log in first"));
        ResponseEntity<ErrorResponse> notFound = handler
                .handleProfileNotFound(new ProfileNotFoundException("Profile not found"));
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthorized.getBody().message()).isEqualTo("Log in first");
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody().message()).isEqualTo("Profile not found");
    }

    @Test
    void find_suspendedStudent_isEmpty() {
        Student bob = new Student("Bob", "bob@tulane.edu", null, null, null);
        bob.setSuspended(true);
        authenticateAs(USER_ID.toString());
        when(authService.findBySupabaseUserId(USER_ID)).thenReturn(Optional.of(bob));
        assertThat(new CurrentStudentResolver(authService).find(new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void requireAdmin_admin_returnsStudent() {
        Student admin = new Student("Admin", "admin@tulane.edu", null, null, null);
        admin.setRole(Role.ADMIN);
        authenticateAs(USER_ID.toString());
        when(authService.findBySupabaseUserId(USER_ID)).thenReturn(Optional.of(admin));
        assertThat(new CurrentStudentResolver(authService).requireAdmin(new MockHttpServletRequest())).isSameAs(admin);
    }

    @Test
    void requireAdmin_student_throwsAdminAccessDenied() {
        authenticateAs(USER_ID.toString());
        when(authService.findBySupabaseUserId(USER_ID))
                .thenReturn(Optional.of(new Student("Bob", "bob@tulane.edu", null, null, null)));
        assertThatThrownBy(() -> new CurrentStudentResolver(authService).requireAdmin(new MockHttpServletRequest()))
                .isInstanceOf(AdminAccessDeniedException.class).hasMessage("Admin access required");
    }

    @Test
    void requireAdmin_notAuthenticated_throwsNotAuthenticated() {
        assertThatThrownBy(() -> new CurrentStudentResolver(authService).requireAdmin(new MockHttpServletRequest()))
                .isInstanceOf(NotAuthenticatedException.class).hasMessage("You must be logged in");
    }
}
