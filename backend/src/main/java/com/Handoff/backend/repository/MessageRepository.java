package com.Handoff.backend.repository;

import com.Handoff.backend.model.Message;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("select m from Message m join fetch m.sender where m.conversation.id = :conversationId "
            + "order by m.createdAt asc, m.id asc")
    List<Message> findByConversation_IdOrderByCreatedAtAscIdAsc(@Param("conversationId") Long conversationId);

    @Query("select m from Message m join fetch m.sender where m.conversation.id = :conversationId "
            + "and m.id > :afterId order by m.createdAt asc, m.id asc")
    List<Message> findByConversation_IdAfterOrdered(@Param("conversationId") Long conversationId,
            @Param("afterId") Long afterId);

    /** The newest message of each given conversation; conversations without messages are absent from the result. */
    @Query("select m from Message m join fetch m.sender where m.id in (select max(m2.id) from Message m2 "
            + "where m2.conversation.id in :conversationIds group by m2.conversation.id)")
    List<Message> findLatestForConversations(@Param("conversationIds") Collection<Long> conversationIds);
}
