package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateListingRequest;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.ListingResponse;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.ForbiddenListingActionException;
import com.Handoff.backend.service.InvalidListingException;
import com.Handoff.backend.service.ListingNotFoundException;
import com.Handoff.backend.service.ListingService;
import com.Handoff.backend.service.NotAuthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/listings")
public class ListingController {

  private final ListingService listingService;
  private final AuthService authService;

  public ListingController(ListingService listingService, AuthService authService) {
    this.listingService = listingService;
    this.authService = authService;
  }

  @GetMapping
  public List<ListingResponse> getFeed() {
    return listingService.getFeed().stream().map(ListingResponse::from).toList();
  }

  @PostMapping
  public ResponseEntity<ListingResponse> createListing(@RequestBody CreateListingRequest request,
                                                         HttpServletRequest httpRequest) {
    Student seller = currentStudent(httpRequest);
    Listing listing = listingService.createListing(seller, request.itemName(), request.description(),
        request.price(), request.condition(), request.category());
    return ResponseEntity.status(HttpStatus.CREATED).body(ListingResponse.from(listing));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id,
                                                        @RequestBody CreateListingRequest request,
                                                        HttpServletRequest httpRequest) {
    Student requester = currentStudent(httpRequest);
    Listing listing = listingService.updateListing(id, requester, request.itemName(), request.description(),
        request.price(), request.condition(), request.category());
    return ResponseEntity.ok(ListingResponse.from(listing));
  }

  private Student currentStudent(HttpServletRequest httpRequest) {
    HttpSession session = httpRequest.getSession(false);
    Long studentId = session != null ? (Long) session.getAttribute(SessionKeys.STUDENT_ID) : null;
    return (studentId != null ? authService.findById(studentId) : Optional.<Student>empty())
        .orElseThrow(NotAuthenticatedException::new);
  }

  @ExceptionHandler(NotAuthenticatedException.class)
  public ResponseEntity<ErrorResponse> handleNotAuthenticated(NotAuthenticatedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(InvalidListingException.class)
  public ResponseEntity<ErrorResponse> handleInvalidListing(InvalidListingException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ListingNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleListingNotFound(ListingNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ForbiddenListingActionException.class)
  public ResponseEntity<ErrorResponse> handleForbiddenListingAction(ForbiddenListingActionException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
  }
}
