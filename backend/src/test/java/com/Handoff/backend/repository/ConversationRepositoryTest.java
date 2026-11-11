package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ConversationRepositoryTest {
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private ListingRepository listingRepository;
  @Autowired private StudentRepository studentRepository;

  @Test
  void findByListingAndBuyer_returnsSavedConversation() {
    Student seller = studentRepository.save(new Student("Seller", "seller@tulane.edu", "hash", null, null));
    Student buyer = studentRepository.save(new Student("Buyer", "buyer@tulane.edu", "hash", null, null));
    Listing listing = listingRepository.save(new Listing("Lamp", "Works", BigDecimal.TEN,
        Condition.GOOD, Category.FURNITURE, seller));
    Conversation saved = conversationRepository.saveAndFlush(new Conversation(listing, buyer));

    assertThat(conversationRepository.findByListing_IdAndBuyer_Id(listing.getId(), buyer.getId()))
        .contains(saved);
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void secondConversationForSameListingAndBuyer_isRejected() {
    Student seller = studentRepository.save(new Student("Seller", "seller2@tulane.edu", "hash", null, null));
    Student buyer = studentRepository.save(new Student("Buyer", "buyer2@tulane.edu", "hash", null, null));
    Listing listing = listingRepository.save(new Listing("Lamp", "Works", BigDecimal.TEN,
        Condition.GOOD, Category.FURNITURE, seller));
    conversationRepository.saveAndFlush(new Conversation(listing, buyer));

    assertThatThrownBy(() -> conversationRepository.saveAndFlush(new Conversation(listing, buyer)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
