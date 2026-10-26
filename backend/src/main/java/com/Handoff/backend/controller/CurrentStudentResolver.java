package com.Handoff.backend.controller;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.NotAuthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the logged-in student from the server-side session, so controllers
 * only handle HTTP routing and never read the session themselves.
 */
@Component
public class CurrentStudentResolver {

  private final AuthService authService;

  public CurrentStudentResolver(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Returns the session's student, if the session still refers to a real one.
   * Never creates a new session.
   */
  public Optional<Student> find(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Long studentId = session != null ? (Long) session.getAttribute(SessionKeys.STUDENT_ID) : null;
    return studentId != null ? authService.findById(studentId) : Optional.empty();
  }

  /**
   * Returns the session's student or throws NotAuthenticatedException (mapped to 401
   * by ApiExceptionHandler) with the given message.
   */
  public Student require(HttpServletRequest request, String errorMessage) {
    return find(request).orElseThrow(() -> new NotAuthenticatedException(errorMessage));
  }
}
