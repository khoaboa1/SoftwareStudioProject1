package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.LoginResponse;
import com.Handoff.backend.dto.ResendPinRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.EmailAlreadyRegisteredException;
import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.InvalidSignupException;
import com.Handoff.backend.service.InvalidVerificationPinException;
import com.Handoff.backend.service.auth.LoginOutcome;
import com.Handoff.backend.service.auth.SupabaseAuthException;
import com.Handoff.backend.service.auth.VerifiedLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Every sign-up and sign-in request from React goes through here; the browser never calls Supabase. */
@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;
  private final CurrentStudentResolver currentStudentResolver;
  private final AuthCookies authCookies;

  public AuthController(AuthService authService, CurrentStudentResolver currentStudentResolver,
                        AuthCookies authCookies) {
    this.authService = authService;
    this.currentStudentResolver = currentStudentResolver;
    this.authCookies = authCookies;
  }

  @PostMapping("/signup")
  public ResponseEntity<Student> signup(@RequestBody SignupRequest request) {
    Student created = authService.signup(request.studentName(), request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
    LoginOutcome outcome = authService.login(request.email(), request.password(), request.deviceId());
    if (outcome.session() != null) {
      authCookies.write(response, outcome.session());
    }
    return ResponseEntity.ok(outcome.response());
  }

  @PostMapping("/verify-pin")
  public ResponseEntity<Student> verifyPin(@RequestBody VerificationRequest request, HttpServletResponse response) {
    VerifiedLogin verified = authService.verifyPin(request.email(), request.pin(), request.deviceId());
    authCookies.write(response, verified.session());
    return ResponseEntity.ok(verified.student());
  }

  @PostMapping("/resend-pin")
  public ResponseEntity<Void> resendPin(@RequestBody ResendPinRequest request) {
    authService.resendPin(request.email());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(
      @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken,
      HttpServletResponse response) {
    try {
      authCookies.write(response, authService.refresh(refreshToken));
      return ResponseEntity.noContent().build();
    } catch (InvalidCredentialsException ex) {
      authCookies.clear(response);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = AuthCookies.ACCESS, required = false) String accessToken,
      HttpServletResponse response) {
    authService.logout(accessToken);
    authCookies.clear(response);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<Student> me(HttpServletRequest httpRequest) {
    return currentStudentResolver.find(httpRequest)
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

  @ExceptionHandler(InvalidVerificationPinException.class)
  public ResponseEntity<ErrorResponse> handleInvalidVerificationPin(InvalidVerificationPinException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(SupabaseAuthException.class)
  public ResponseEntity<ErrorResponse> handleSupabaseAuth(SupabaseAuthException ex) {
    if (ex.getStatus() == 429) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .body(new ErrorResponse("Too many requests. Please wait a minute and try again."));
    }
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("Authentication service is unavailable. Please try again."));
  }
}
