package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.service.NotAuthenticatedException;
import com.Handoff.backend.service.ProfileNotFoundException;
import com.Handoff.backend.service.InvalidImageException;
import com.Handoff.backend.service.storage.ImageStorageException;
import com.Handoff.backend.service.storage.ImageStorageNotConfiguredException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(NotAuthenticatedException.class)
  public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler({InvalidImageException.class, MaxUploadSizeExceededException.class})
  public ResponseEntity<ErrorResponse> handleInvalidImage(Exception ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse("Photos must be JPEG, PNG or WebP and at most 5 MB"));
  }

  @ExceptionHandler(ImageStorageNotConfiguredException.class)
  public ResponseEntity<ErrorResponse> handleStorageNotConfigured(ImageStorageNotConfiguredException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ImageStorageException.class)
  public ResponseEntity<ErrorResponse> handleStorageFailure(ImageStorageException ex) {
    log.warn("Photo storage failed with status {}", ex.getStatusCode());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("Couldn't store the photo. Please try again."));
  }
}
