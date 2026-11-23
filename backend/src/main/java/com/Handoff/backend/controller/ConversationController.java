package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ConversationResponse;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.ConversationAccessDeniedException;
import com.Handoff.backend.service.ConversationNotFoundException;
import com.Handoff.backend.service.ConversationService;
import com.Handoff.backend.service.ListingNotFoundException;
import com.Handoff.backend.service.ListingSoldException;
import com.Handoff.backend.service.MessageService;
import com.Handoff.backend.service.OwnListingConversationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CurrentStudentResolver currentStudentResolver;

    public ConversationController(ConversationService conversationService, MessageService messageService,
            CurrentStudentResolver currentStudentResolver) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.currentStudentResolver = currentStudentResolver;
    }

    @PostMapping("/listings/{id}/conversation")
    public ResponseEntity<ConversationResponse> startConversation(@PathVariable Long id, HttpServletRequest request) {
        Student buyer = currentStudentResolver.require(request, "You must be logged in to message a seller");
        var result = conversationService.startOrReopen(id, buyer);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ConversationResponse.from(result.conversation()));
    }

    @GetMapping("/api/conversations")
    public List<ConversationResponse> listConversations(HttpServletRequest request) {
        Student student = currentStudentResolver.require(request, "You must be logged in to view conversations");
        var conversations = conversationService.listForStudent(student);
        var latest = messageService.latestByConversation(conversations);
        return conversations.stream().map(c -> ConversationResponse.from(c, latest.get(c.getId()))).toList();
    }

    @GetMapping("/api/conversations/{conversationId}")
    public ConversationResponse getConversation(@PathVariable Long conversationId, HttpServletRequest request) {
        Student student = currentStudentResolver.require(request, "You must be logged in to view conversations");
        var conversation = conversationService.getForParticipant(conversationId, student);
        return ConversationResponse.from(conversation,
                messageService.latestByConversation(List.of(conversation)).get(conversation.getId()));
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

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleConversationAccessDenied(ConversationAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }
}
