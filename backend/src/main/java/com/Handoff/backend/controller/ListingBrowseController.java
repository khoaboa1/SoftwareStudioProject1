package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.ListingBrowseRequest;
import com.Handoff.backend.dto.ListingPageResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.InvalidListingException;
import com.Handoff.backend.service.ListingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
public class ListingBrowseController {

  private static final String NOT_AUTHENTICATED = "You must be logged in to view marketplace listings";

  private final ListingService listingService;
  private final CurrentStudentResolver currentStudentResolver;

  public ListingBrowseController(ListingService listingService,
                                 CurrentStudentResolver currentStudentResolver) {
    this.listingService = listingService;
    this.currentStudentResolver = currentStudentResolver;
  }

  @GetMapping
  public ListingPageResponse browse(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String minPrice,
      @RequestParam(required = false) String maxPrice,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder,
      @RequestParam(required = false) String page,
      @RequestParam(required = false) String limit,
      HttpServletRequest httpRequest) {
    Student requester = currentStudentResolver.require(httpRequest, NOT_AUTHENTICATED);
    ListingBrowseRequest browseRequest = ListingBrowseRequest.parse(
        q, category, minPrice, maxPrice, sortBy, sortOrder, page, limit);
    return listingService.browseListings(requester, browseRequest);
  }

  @ExceptionHandler(InvalidListingException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidListingException exception) {
    return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
  }
}
