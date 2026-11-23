package com.Handoff.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConversationResponseTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    private ConversationResponse response(ConversationResponse.LastMessage lastMessage) {
        return new ConversationResponse(1L, 2L, "Desk Lamp", 3L, "Buyer", 4L, "Seller",
                Instant.parse("2026-11-11T20:00:00Z"), "ACTIVE", lastMessage);
    }

    @Test
    void lastMessageIsSerializedWithContentSenderAndTime() throws Exception {
        ConversationResponse.LastMessage last = new ConversationResponse.LastMessage("Is it still available?", 3L,
                Instant.parse("2026-11-11T20:05:00Z"));

        JsonNode tree = mapper.readTree(mapper.writeValueAsString(response(last)));

        assertThat(tree.get("lastMessage").get("content").asString()).isEqualTo("Is it still available?");
        assertThat(tree.get("lastMessage").get("senderId").asLong()).isEqualTo(3L);
        assertThat(tree.get("lastMessage").has("createdAt")).isTrue();
        assertThat(tree.get("listingStatus").asString()).isEqualTo("ACTIVE");
    }

    private com.Handoff.backend.model.Conversation conversation() {
        com.Handoff.backend.model.Student seller = new com.Handoff.backend.model.Student("Seller", "seller@tulane.edu",
                "hash", null, null);
        seller.setId(4L);
        com.Handoff.backend.model.Student buyer = new com.Handoff.backend.model.Student("Buyer", "buyer@tulane.edu",
                "hash", null, null);
        buyer.setId(3L);
        com.Handoff.backend.model.Listing listing = new com.Handoff.backend.model.Listing("Lamp", "Works",
                java.math.BigDecimal.TEN, com.Handoff.backend.model.Condition.GOOD,
                com.Handoff.backend.model.Category.FURNITURE, seller);
        return new com.Handoff.backend.model.Conversation(listing, buyer);
    }

    @Test
    void from_withLatestMessage_fillsLastMessage() {
        com.Handoff.backend.model.Conversation conversation = conversation();
        com.Handoff.backend.model.Message latest = new com.Handoff.backend.model.Message(conversation,
                conversation.getBuyer(), "Is it still available?");

        ConversationResponse response = ConversationResponse.from(conversation, latest);

        assertThat(response.lastMessage().content()).isEqualTo("Is it still available?");
        assertThat(response.lastMessage().senderId()).isEqualTo(3L);
        assertThat(response.lastMessage().createdAt()).isEqualTo(latest.getCreatedAt());
    }

    @Test
    void from_withoutLatestMessage_leavesLastMessageNull() {
        assertThat(ConversationResponse.from(conversation(), null).lastMessage()).isNull();
        assertThat(ConversationResponse.from(conversation()).lastMessage()).isNull();
    }

    @Test
    void missingLastMessageIsSerializedAsNull() throws Exception {
        JsonNode tree = mapper.readTree(mapper.writeValueAsString(response(null)));

        assertThat(tree.get("lastMessage").isNull()).isTrue();
    }
}
