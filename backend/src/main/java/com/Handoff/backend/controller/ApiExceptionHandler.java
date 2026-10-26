package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.service.NotAuthenticatedException;
import com.Handoff.backend.service.ProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handlers shared by every controller. Controller-specific errors
 * stay in their own controller.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotAuthenticatedException.class)
  public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }
}
