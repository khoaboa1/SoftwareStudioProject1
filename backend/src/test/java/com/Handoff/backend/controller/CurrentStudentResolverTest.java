package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.NotAuthenticatedException;
import com.Handoff.backend.service.ProfileNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentStudentResolverTest {

  @Mock
  private AuthService authService;

  private MockHttpServletRequest requestWithSession(Long studentId) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true).setAttribute(SessionKeys.STUDENT_ID, studentId);
    return request;
  }

  @Test
  void require_returnsStudentFromSession() {
    Student bob = new Student("Bob", "bob@tulane.edu", "hashed", null, null);
    when(authService.findById(7L)).thenReturn(Optional.of(bob));
    CurrentStudentResolver resolver = new CurrentStudentResolver(authService);

    assertThat(resolver.require(requestWithSession(7L), "Log in first")).isSameAs(bob);
  }

  @Test
  void require_throwsWithGivenMessage_whenNoSession() {
    CurrentStudentResolver resolver = new CurrentStudentResolver(authService);
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThatThrownBy(() -> resolver.require(request, "Log in first"))
        .isInstanceOf(NotAuthenticatedException.class)
        .hasMessage("Log in first");
    // Must not create a session as a side effect.
    assertThat(request.getSession(false)).isNull();
    verifyNoInteractions(authService);
  }

  @Test
  void require_throws_whenSessionStudentNoLongerExists() {
    when(authService.findById(7L)).thenReturn(Optional.empty());
    CurrentStudentResolver resolver = new CurrentStudentResolver(authService);

    assertThatThrownBy(() -> resolver.require(requestWithSession(7L), "Log in first"))
        .isInstanceOf(NotAuthenticatedException.class);
  }

  @Test
  void find_returnsEmpty_whenSessionHasNoStudentId() {
    CurrentStudentResolver resolver = new CurrentStudentResolver(authService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true);

    assertThat(resolver.find(request)).isEmpty();
    verifyNoInteractions(authService);
  }

  @Test
  void apiExceptionHandler_mapsSharedExceptionsToStatusAndMessage() {
    ApiExceptionHandler handler = new ApiExceptionHandler();

    ResponseEntity<ErrorResponse> unauthorized =
        handler.handleNotAuthenticated(new NotAuthenticatedException("Log in first"));
    ResponseEntity<ErrorResponse> notFound =
        handler.handleProfileNotFound(new ProfileNotFoundException("Profile not found"));

    assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(unauthorized.getBody().message()).isEqualTo("Log in first");
    assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(notFound.getBody().message()).isEqualTo("Profile not found");
  }
}
