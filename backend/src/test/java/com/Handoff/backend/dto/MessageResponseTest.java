package com.Handoff.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Message;
import com.Handoff.backend.model.Student;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MessageResponseTest {
    @Test
    void from_mapsSenderContentAndTimestamp() {
        Student seller = new Student("Seller", "seller@tulane.edu", "hash", null, null);
        seller.setId(4L);
        Student buyer = new Student("Buyer", "buyer@tulane.edu", "hash", null, null);
        buyer.setId(7L);
        Listing listing = new Listing("Lamp", "Works", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller);
        Conversation conversation = new Conversation(listing, buyer);
        conversation.setId(3L);
        Message message = new Message(conversation, buyer, "Is it still available?");
        message.setId(11L);

        MessageResponse response = MessageResponse.from(message);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.conversationId()).isEqualTo(3L);
        assertThat(response.senderId()).isEqualTo(7L);
        assertThat(response.senderName()).isEqualTo("Buyer");
        assertThat(response.content()).isEqualTo("Is it still available?");
        assertThat(response.createdAt()).isNotNull();
    }
}
