package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateProfileRequest;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.DuplicateProfileException;
import com.Handoff.backend.service.NotAuthenticatedException;
import com.Handoff.backend.service.ProfileNotFoundException;
import com.Handoff.backend.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Controller exposing REST endpoints for student profile management.
 */
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

  private final ProfileService profileService;
  private final AuthService authService;

  public ProfileController(ProfileService profileService, AuthService authService) {
    this.profileService = profileService;
    this.authService = authService;
  }

  /**
   * Retrieves the profile of the currently authenticated student.
   *
   * @param httpRequest HTTP servlet request to access the user session
   * @return 200 OK with the student's Profile, or 404 if no profile exists
   */
  @GetMapping("/me")
  public ResponseEntity<Profile> getMyProfile(HttpServletRequest httpRequest) {
    Student student = currentStudent(httpRequest);
    Profile profile = profileService.getMyProfile(student);
    return ResponseEntity.ok(profile);
  }

  /**
   * Generates a profile for the currently authenticated student.
   *
   * @param request     incoming payload containing name, major, and bio
   * @param httpRequest HTTP servlet request to access the user session
   * @return 201 Created status and newly created Profile
   */
  @PostMapping
  public ResponseEntity<Profile> createProfile(@Valid @RequestBody CreateProfileRequest request,
                                               HttpServletRequest httpRequest) {
    // Authenticate and retrieve current session student
    Student student = currentStudent(httpRequest);

    // Delegate creation to service layer
    Profile createdProfile = profileService.createProfile(
        student,
        request.name(),
        request.major(),
        request.bio()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(createdProfile);
  }

  /**
   * Helper method to extract the authenticated Student from the HTTP session.
   */
  private Student currentStudent(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    Long studentId = session != null ? (Long) session.getAttribute(SessionKeys.STUDENT_ID) : null;
    return (studentId != null ? authService.findById(studentId) : Optional.<Student>empty())
        .orElseThrow(() -> new NotAuthenticatedException("User must be authenticated to create a profile."));
  }

  /**
   * Handles unauthenticated requests with HTTP 401.
   */
  @ExceptionHandler(NotAuthenticatedException.class)
  public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
  }

  /**
   * Handles duplicate profile creations with HTTP 409 Conflict.
   */
  @ExceptionHandler(DuplicateProfileException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateProfile(DuplicateProfileException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
  }

  /**
   * Handles profile not found with HTTP 404 Not Found.
   */
  @ExceptionHandler(ProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }

  /**
   * Handles bean validation constraint failures with HTTP 400 Bad Request.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationFailure(MethodArgumentNotValidException ex) {
    String errorMsg = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .findFirst()
        .orElse("Validation failed");
    return ResponseEntity.badRequest().body(new ErrorResponse(errorMsg));
  }

  /**
   * Handles missing or malformed JSON request bodies with HTTP 400 Bad Request.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse("Request body is missing or malformed."));
  }
}
