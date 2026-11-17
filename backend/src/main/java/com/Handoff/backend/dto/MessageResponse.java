package com.Handoff.backend.dto;

import com.Handoff.backend.model.Message;
import java.time.Instant;

public record MessageResponse(Long id, Long conversationId, Long senderId, String senderName, String content,
        Instant createdAt) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getConversation().getId(), message.getSender().getId(),
                message.getSender().getStudentName(), message.getContent(), message.getCreatedAt());
    }
}
