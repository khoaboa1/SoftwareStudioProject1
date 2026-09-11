package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.EmailAlreadyRegisteredException;
import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.InvalidSignupException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private static final String SESSION_STUDENT_ID = "studentId";

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public ResponseEntity<Student> signup(@RequestBody SignupRequest request) {
    Student created = authService.signup(request.studentName(), request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PostMapping("/login")
  public ResponseEntity<Student> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    Student student = authService.login(request.email(), request.password());
    HttpSession session = httpRequest.getSession(true);
    session.setAttribute(SESSION_STUDENT_ID, student.getId());
    return ResponseEntity.ok(student);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<Student> me(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    Long studentId = session != null ? (Long) session.getAttribute(SESSION_STUDENT_ID) : null;
    if (studentId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return authService.findById(studentId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @ExceptionHandler(InvalidSignupException.class)
  public ResponseEntity<ErrorResponse> handleInvalidSignup(InvalidSignupException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponse> handleEmailTaken(EmailAlreadyRegisteredException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
  }
}
