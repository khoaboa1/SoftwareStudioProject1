package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ConversationResponse;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.ConversationService;
import com.Handoff.backend.service.ListingNotFoundException;
import com.Handoff.backend.service.ListingSoldException;
import com.Handoff.backend.service.OwnListingConversationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
public class ConversationController {
  private final ConversationService conversationService;
  private final CurrentStudentResolver currentStudentResolver;

  public ConversationController(ConversationService conversationService,
                                CurrentStudentResolver currentStudentResolver) {
    this.conversationService = conversationService;
    this.currentStudentResolver = currentStudentResolver;
  }

  @PostMapping("/{id}/conversation")
  public ResponseEntity<ConversationResponse> startConversation(@PathVariable Long id,
                                                                 HttpServletRequest request) {
    Student buyer = currentStudentResolver.require(request, "You must be logged in to message a seller");
    var result = conversationService.startOrReopen(id, buyer);
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(ConversationResponse.from(result.conversation()));
  }

  @ExceptionHandler(ListingNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleListingNotFound(ListingNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(OwnListingConversationException.class)
  public ResponseEntity<ErrorResponse> handleOwnListing(OwnListingConversationException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(ListingSoldException.class)
  public ResponseEntity<ErrorResponse> handleSoldListing(ListingSoldException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
  }
}
