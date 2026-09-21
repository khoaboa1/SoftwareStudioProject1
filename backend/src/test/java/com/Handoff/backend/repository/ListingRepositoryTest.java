package com.Handoff.backend.repository;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ListingRepositoryTest {

  @Autowired
  private ListingRepository listingRepository;

  @Autowired
  private StudentRepository studentRepository;

  @Test
  void findAllByOrderByCreatedAtDesc_returnsNewestFirst() {
    Student seller = studentRepository.save(new Student(
        "Sarah", "sarah@tulane.edu", "hashed", null, null));

    Listing older = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"),
        Condition.GOOD, Category.FURNITURE, seller);
    older.setCreatedAt(Instant.now().minusSeconds(60));
    listingRepository.save(older);

    Listing newer = new Listing("Mini Fridge", "Barely used", new BigDecimal("40.00"),
        Condition.LIKE_NEW, Category.KITCHEN, seller);
    newer.setCreatedAt(Instant.now());
    listingRepository.save(newer);

    List<Listing> listings = listingRepository.findAllByOrderByCreatedAtDesc();

    assertThat(listings).extracting(Listing::getItemName)
        .containsExactly("Mini Fridge", "Desk Lamp");
  }
}
