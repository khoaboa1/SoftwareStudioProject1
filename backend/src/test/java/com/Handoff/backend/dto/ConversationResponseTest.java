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

    @Test
    void missingLastMessageIsSerializedAsNull() throws Exception {
        JsonNode tree = mapper.readTree(mapper.writeValueAsString(response(null)));

        assertThat(tree.get("lastMessage").isNull()).isTrue();
    }
}
