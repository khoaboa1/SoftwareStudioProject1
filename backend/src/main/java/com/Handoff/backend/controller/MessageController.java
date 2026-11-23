package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.MessageResponse;
import com.Handoff.backend.dto.SendMessageRequest;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.ConversationAccessDeniedException;
import com.Handoff.backend.service.ConversationNotFoundException;
import com.Handoff.backend.service.InvalidMessageException;
import com.Handoff.backend.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {
    private final MessageService messageService;
    private final CurrentStudentResolver currentStudentResolver;

    public MessageController(MessageService messageService, CurrentStudentResolver currentStudentResolver) {
        this.messageService = messageService;
        this.currentStudentResolver = currentStudentResolver;
    }

    @PostMapping("/api/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(@PathVariable Long conversationId,
            @RequestBody SendMessageRequest body, HttpServletRequest request) {
        Student sender = currentStudentResolver.require(request, "You must be logged in to send messages");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.from(messageService.send(conversationId, sender, body.content())));
    }

    @GetMapping("/api/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(@PathVariable Long conversationId,
            @RequestParam(required = false) Long after, HttpServletRequest request) {
        Student student = currentStudentResolver.require(request, "You must be logged in to view messages");
        return messageService.history(conversationId, student, after).stream().map(MessageResponse::from).toList();
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConversationAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleConversationAccessDenied(ConversationAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidMessageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMessage(InvalidMessageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }
}
