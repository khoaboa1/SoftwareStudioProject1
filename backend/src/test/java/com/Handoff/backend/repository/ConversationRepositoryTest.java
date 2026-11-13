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

  private Student student(String name, String email) {
    return studentRepository.save(new Student(name, email, "hash", null, null));
  }

  private Listing listing(String itemName, Student seller) {
    return listingRepository.save(new Listing(itemName, "Works", BigDecimal.TEN,
        Condition.GOOD, Category.FURNITURE, seller));
  }

  @Test
  void findAllForStudent_returnsBuyerAndSellerSides_only() {
    Student seller = student("Seller", "seller3@tulane.edu");
    Student buyer = student("Buyer", "buyer3@tulane.edu");
    Student other = student("Other", "other3@tulane.edu");
    Student nobody = student("Nobody", "nobody3@tulane.edu");
    Listing sellersLamp = listing("Lamp", seller);
    Listing buyersDesk = listing("Desk", buyer);
    Conversation buyerAsks = conversationRepository.saveAndFlush(new Conversation(sellersLamp, buyer));
    Conversation otherAsks = conversationRepository.saveAndFlush(new Conversation(buyersDesk, other));

    assertThat(conversationRepository.findAllForStudent(buyer.getId()))
        .containsExactlyInAnyOrder(buyerAsks, otherAsks);
    assertThat(conversationRepository.findAllForStudent(seller.getId())).containsExactly(buyerAsks);
    assertThat(conversationRepository.findAllForStudent(other.getId())).containsExactly(otherAsks);
    assertThat(conversationRepository.findAllForStudent(nobody.getId())).isEmpty();
  }

  @Test
  void findWithParticipantsById_returnsConversationWithListingAndBuyer() {
    Student seller = student("Seller", "seller4@tulane.edu");
    Student buyer = student("Buyer", "buyer4@tulane.edu");
    Conversation saved = conversationRepository.saveAndFlush(new Conversation(listing("Lamp", seller), buyer));

    var found = conversationRepository.findWithParticipantsById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getListing().getSeller().getId()).isEqualTo(seller.getId());
    assertThat(found.get().getBuyer().getId()).isEqualTo(buyer.getId());
    assertThat(conversationRepository.findWithParticipantsById(999999L)).isEmpty();
  }
}
