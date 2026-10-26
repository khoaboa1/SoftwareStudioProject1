package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateListingRequest;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.ListingResponse;
import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.ForbiddenListingActionException;
import com.Handoff.backend.service.InvalidListingException;
import com.Handoff.backend.service.ListingNotFoundException;
import com.Handoff.backend.service.ListingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/listings")
public class ListingController {

  private static final String NOT_AUTHENTICATED_TO_VIEW = "You must be logged in to view marketplace listings";
  // Preserve existing authentication errors for listing mutations.
  private static final String NOT_AUTHENTICATED_TO_POST = "You must be logged in to post a listing";

  private final ListingService listingService;
  private final CurrentStudentResolver currentStudentResolver;

  public ListingController(ListingService listingService, CurrentStudentResolver currentStudentResolver) {
    this.listingService = listingService;
    this.currentStudentResolver = currentStudentResolver;
  }

  @GetMapping
  public List<ListingResponse> getFeed(HttpServletRequest httpRequest) {
    // Only the server-side session determines identity; domain query parameters are never bound.
    Student requester = currentStudentResolver.require(httpRequest, NOT_AUTHENTICATED_TO_VIEW);
    return listingService.getFeed(requester).stream().map(ListingResponse::from).toList();
  }

  @GetMapping("/{id}")
  public ListingResponse getListing(@PathVariable Long id, HttpServletRequest httpRequest) {
    Student requester = currentStudentResolver.require(httpRequest, NOT_AUTHENTICATED_TO_VIEW);
    return ListingResponse.from(listingService.getListing(id, requester));
  }

  @GetMapping("/categories")
  public List<String> getCategories() {
    return Arrays.stream(Category.values()).map(category -> category.name()).toList();
  }

  @PostMapping
  public ResponseEntity<ListingResponse> createListing(@RequestBody CreateListingRequest request,
                                                         HttpServletRequest httpRequest) {
    Student seller = currentStudentResolver.require(httpRequest, NOT_AUTHENTICATED_TO_POST);
    Listing listing = listingService.createListing(seller, request.itemName(), request.description(),
        request.price(), request.condition(), request.category());
    return ResponseEntity.status(HttpStatus.CREATED).body(ListingResponse.from(listing));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ListingResponse> updateListing(@PathVariable Long id,
                                                        @RequestBody CreateListingRequest request,
                                                        HttpServletRequest httpRequest) {
    Student requester = currentStudentResolver.require(httpRequest, NOT_AUTHENTICATED_TO_POST);
    Listing listing = listingService.updateListing(id, requester, request.itemName(), request.description(),
        request.price(), request.condition(), request.category());
    return ResponseEntity.ok(ListingResponse.from(listing));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteListing(@PathVariable Long id, HttpServletRequest httpRequest) {
    Student requester = currentStudentResolver.require(httpRequest, NOT_AUTHENTICATED_TO_POST);
    listingService.deleteListing(id, requester);
    return ResponseEntity.noContent().build();
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
