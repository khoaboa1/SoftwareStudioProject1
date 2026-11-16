package com.Handoff.backend.dto;

import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Message;
import java.time.Instant;

public record ConversationResponse(Long id, Long listingId, String itemName, Long buyerId, String buyerName,
        Long sellerId, String sellerName, Instant createdAt, String listingStatus, LastMessage lastMessage) {
    public record LastMessage(String content, Long senderId, Instant createdAt) {
    }

    public static ConversationResponse from(Conversation conversation) {
        return from(conversation, null);
    }

    public static ConversationResponse from(Conversation conversation, Message latest) {
        Listing listing = conversation.getListing();
        return new ConversationResponse(conversation.getId(), listing.getId(), listing.getItemName(),
                conversation.getBuyer().getId(), conversation.getBuyer().getStudentName(), listing.getSeller().getId(),
                listing.getSeller().getStudentName(), conversation.getCreatedAt(),
                listing.getStatus() == null ? null : listing.getStatus().name(), latest == null ? null
                        : new LastMessage(latest.getContent(), latest.getSender().getId(), latest.getCreatedAt()));
    }
}
