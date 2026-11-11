package com.Handoff.backend.repository;

import com.Handoff.backend.model.Conversation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
  Optional<Conversation> findByListing_IdAndBuyer_Id(Long listingId, Long buyerId);
}
