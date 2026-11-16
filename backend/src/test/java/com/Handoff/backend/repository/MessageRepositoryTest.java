package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Message;
import com.Handoff.backend.model.Student;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class MessageRepositoryTest {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private StudentRepository studentRepository;

    private Student student(String name, String email) {
        return studentRepository.save(new Student(name, email, "hash", null, null));
    }

    private Conversation conversation(String itemName, Student seller, Student buyer) {
        Listing listing = listingRepository
                .save(new Listing(itemName, "Works", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller));
        return conversationRepository.saveAndFlush(new Conversation(listing, buyer));
    }

    @Test
    void findByConversationOrdered_returnsOldestFirst() {
        Student seller = student("Seller", "msg-seller1@tulane.edu");
        Student buyer = student("Buyer", "msg-buyer1@tulane.edu");
        Conversation conversation = conversation("Lamp", seller, buyer);
        Message first = messageRepository.saveAndFlush(new Message(conversation, buyer, "first"));
        Message second = messageRepository.saveAndFlush(new Message(conversation, seller, "second"));
        Message third = messageRepository.saveAndFlush(new Message(conversation, buyer, "third"));

        assertThat(messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(conversation.getId()))
                .containsExactly(first, second, third);
    }

    @Test
    void findByConversationOrdered_excludesOtherConversations() {
        Student seller = student("Seller", "msg-seller2@tulane.edu");
        Student buyer = student("Buyer", "msg-buyer2@tulane.edu");
        Conversation lamp = conversation("Lamp", seller, buyer);
        Conversation desk = conversation("Desk", seller, buyer);
        Message lampMessage = messageRepository.saveAndFlush(new Message(lamp, buyer, "about the lamp"));
        Message deskMessage = messageRepository.saveAndFlush(new Message(desk, buyer, "about the desk"));

        assertThat(messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(lamp.getId()))
                .containsExactly(lampMessage);
        assertThat(messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(desk.getId()))
                .containsExactly(deskMessage);
    }

    @Test
    void findByConversationAfter_returnsOnlyNewerMessagesOldestFirst() {
        Student seller = student("Seller", "msg-seller4@tulane.edu");
        Student buyer = student("Buyer", "msg-buyer4@tulane.edu");
        Conversation conversation = conversation("Lamp", seller, buyer);
        Message first = messageRepository.saveAndFlush(new Message(conversation, buyer, "first"));
        Message second = messageRepository.saveAndFlush(new Message(conversation, seller, "second"));
        Message third = messageRepository.saveAndFlush(new Message(conversation, buyer, "third"));

        assertThat(messageRepository.findByConversation_IdAfterOrdered(conversation.getId(), first.getId()))
                .containsExactly(second, third);
        assertThat(messageRepository.findByConversation_IdAfterOrdered(conversation.getId(), third.getId())).isEmpty();
    }

    @Test
    void newMessage_hasCreatedAtAndGetters() {
        Student seller = student("Seller", "msg-seller3@tulane.edu");
        Student buyer = student("Buyer", "msg-buyer3@tulane.edu");
        Conversation conversation = conversation("Lamp", seller, buyer);

        Message saved = messageRepository.saveAndFlush(new Message(conversation, buyer, "hello"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getConversation()).isEqualTo(conversation);
        assertThat(saved.getSender()).isEqualTo(buyer);
        assertThat(saved.getContent()).isEqualTo("hello");
    }
}
