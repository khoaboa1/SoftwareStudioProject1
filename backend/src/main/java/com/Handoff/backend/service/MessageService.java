package com.Handoff.backend.service;

import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Message;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.MessageRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    static final int MAX_CONTENT_LENGTH = 2000;

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;

    public MessageService(ConversationService conversationService, MessageRepository messageRepository) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
    }

    public Message send(Long conversationId, Student sender, String content) {
        Conversation conversation = conversationService.getForParticipant(conversationId, sender);
        String trimmed = content == null ? "" : content.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidMessageException("Message content is required");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidMessageException("Message content must be at most " + MAX_CONTENT_LENGTH + " characters");
        }
        return messageRepository.save(new Message(conversation, sender, trimmed));
    }

    /** Messages oldest first; when {@code afterId} is given, only those with a larger id (the polling cursor). */
    /** Newest message per conversation id, for the inbox's last-message preview. */
    public Map<Long, Message> latestByConversation(Collection<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = conversations.stream().map(Conversation::getId).toList();
        return messageRepository.findLatestForConversations(ids).stream()
                .collect(Collectors.toMap(message -> message.getConversation().getId(), Function.identity()));
    }

    public List<Message> history(Long conversationId, Student student, Long afterId) {
        conversationService.getForParticipant(conversationId, student);
        if (afterId != null) {
            return messageRepository.findByConversation_IdAfterOrdered(conversationId, afterId);
        }
        return messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(conversationId);
    }
}
