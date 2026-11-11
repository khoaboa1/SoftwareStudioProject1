package com.Handoff.backend.dto;

import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import java.time.Instant;

public record ConversationResponse(Long id, Long listingId, String itemName,
                                   Long buyerId, String buyerName,
                                   Long sellerId, String sellerName, Instant createdAt) {
  public static ConversationResponse from(Conversation conversation) {
    Listing listing = conversation.getListing();
    return new ConversationResponse(conversation.getId(), listing.getId(), listing.getItemName(),
        conversation.getBuyer().getId(), conversation.getBuyer().getStudentName(),
        listing.getSeller().getId(), listing.getSeller().getStudentName(), conversation.getCreatedAt());
  }
}
