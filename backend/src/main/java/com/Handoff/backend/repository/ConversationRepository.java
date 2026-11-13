package com.Handoff.backend.repository;

import com.Handoff.backend.model.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
  Optional<Conversation> findByListing_IdAndBuyer_Id(Long listingId, Long buyerId);

  @Query("select c from Conversation c join fetch c.listing l join fetch l.seller join fetch c.buyer "
      + "where c.buyer.id = :studentId or l.seller.id = :studentId order by c.createdAt desc, c.id desc")
  List<Conversation> findAllForStudent(@Param("studentId") Long studentId);

  @Query("select c from Conversation c join fetch c.listing l join fetch l.seller join fetch c.buyer where c.id = :id")
  Optional<Conversation> findWithParticipantsById(@Param("id") Long id);
}
