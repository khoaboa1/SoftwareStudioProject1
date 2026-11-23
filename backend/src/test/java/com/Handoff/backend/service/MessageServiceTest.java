package com.Handoff.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Message;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.MessageRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock
    private ConversationService conversationService;
    @Mock
    private MessageRepository messageRepository;
    private Student buyer;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        buyer = new Student("Buyer", "buyer@tulane.edu", "hash", null, null);
        buyer.setId(1L);
        Student seller = new Student("Seller", "seller@tulane.edu", "hash", null, null);
        seller.setId(2L);
        Listing listing = new Listing("Lamp", "Works", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller);
        conversation = new Conversation(listing, buyer);
        conversation.setId(5L);
    }

    private MessageService service() {
        return new MessageService(conversationService, messageRepository);
    }

    private void participant() {
        when(conversationService.getForParticipant(5L, buyer)).thenReturn(conversation);
    }

    private void saves() {
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void send_savesTrimmedMessageAndReturnsIt() {
        participant();
        saves();

        Message sent = service().send(5L, buyer, "  hi  ");

        assertThat(sent.getContent()).isEqualTo("hi");
        assertThat(sent.getSender()).isEqualTo(buyer);
        assertThat(sent.getConversation()).isEqualTo(conversation);
        verify(messageRepository).save(sent);
    }

    @Test
    void send_blankContent_throwsInvalidMessage() {
        participant();

        assertThatThrownBy(() -> service().send(5L, buyer, "   ")).isInstanceOf(InvalidMessageException.class)
                .hasMessage("Message content is required");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_nullContent_throwsInvalidMessage() {
        participant();

        assertThatThrownBy(() -> service().send(5L, buyer, null)).isInstanceOf(InvalidMessageException.class)
                .hasMessage("Message content is required");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_tooLong_throwsInvalidMessage() {
        participant();

        assertThatThrownBy(() -> service().send(5L, buyer, "a".repeat(2001)))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessage("Message content must be at most 2000 characters");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void send_exactly2000Chars_isAccepted() {
        participant();
        saves();

        Message sent = service().send(5L, buyer, "a".repeat(2000));

        assertThat(sent.getContent()).hasSize(2000);
    }

    @Test
    void send_nonParticipant_propagatesAccessDenied() {
        when(conversationService.getForParticipant(5L, buyer)).thenThrow(new ConversationAccessDeniedException());

        assertThatThrownBy(() -> service().send(5L, buyer, "hi")).isInstanceOf(ConversationAccessDeniedException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void history_returnsMessagesAfterParticipantCheck() {
        participant();
        List<Message> messages = List.of(new Message(conversation, buyer, "one"),
                new Message(conversation, buyer, "two"));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(5L)).thenReturn(messages);

        assertThat(service().history(5L, buyer, null)).isEqualTo(messages);
    }

    @Test
    void history_withCursor_returnsOnlyNewerMessages() {
        participant();
        List<Message> newer = List.of(new Message(conversation, buyer, "newer"));
        when(messageRepository.findByConversation_IdAfterOrdered(5L, 9L)).thenReturn(newer);

        assertThat(service().history(5L, buyer, 9L)).isEqualTo(newer);
        verify(messageRepository, never()).findByConversation_IdOrderByCreatedAtAscIdAsc(any());
    }

    @Test
    void latestByConversation_mapsConversationIdToNewestMessage() {
        Message last = new Message(conversation, buyer, "last");
        when(messageRepository.findLatestForConversations(List.of(5L))).thenReturn(List.of(last));

        assertThat(service().latestByConversation(List.of(conversation))).containsOnlyKeys(5L).containsEntry(5L, last);
    }

    @Test
    void latestByConversation_noConversations_returnsEmptyWithoutQuerying() {
        assertThat(service().latestByConversation(List.of())).isEmpty();
        verify(messageRepository, never()).findLatestForConversations(any());
    }

    @Test
    void history_nonParticipant_propagatesAccessDenied() {
        when(conversationService.getForParticipant(5L, buyer)).thenThrow(new ConversationAccessDeniedException());

        assertThatThrownBy(() -> service().history(5L, buyer, null))
                .isInstanceOf(ConversationAccessDeniedException.class);
        verify(messageRepository, never()).findByConversation_IdOrderByCreatedAtAscIdAsc(any());
    }

    @Test
    void send_onSoldListing_isSaved() {
        conversation.getListing().setStatus(ListingStatus.SOLD);
        participant();
        saves();

        Message sent = service().send(5L, buyer, "Still meeting?");

        assertThat(sent.getContent()).isEqualTo("Still meeting?");
        assertThat(sent.getConversation()).isEqualTo(conversation);
        verify(conversationService).getForParticipant(5L, buyer);
    }

    @Test
    void history_onSoldListing_isReturned() {
        conversation.getListing().setStatus(ListingStatus.SOLD);
        participant();
        List<Message> messages = List.of(new Message(conversation, buyer, "one"));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(5L)).thenReturn(messages);

        assertThat(service().history(5L, buyer, null)).isEqualTo(messages);
        verify(conversationService).getForParticipant(5L, buyer);
    }
}
