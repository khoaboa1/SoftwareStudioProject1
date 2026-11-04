package com.Handoff.backend.repository;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.model.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import jakarta.persistence.EntityManager;

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

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  void replaceImages_persistsInOrder_andDeleteCascades() {
    Student seller = studentRepository.save(new Student("Sarah", "sarah2@tulane.edu", "hashed", null, null));
    Listing listing = new Listing("Lamp", "Works", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller);
    listing.replaceImages(List.of("https://a", "https://b", "https://c"));
    Long id = listingRepository.saveAndFlush(listing).getId();
    entityManager.clear();
    Listing loaded = listingRepository.findById(id).orElseThrow();
    assertThat(loaded.getImageUrls()).containsExactly("https://a", "https://b", "https://c");
    loaded.replaceImages(List.of("https://b"));
    listingRepository.saveAndFlush(loaded);
    entityManager.clear();
    assertThat(listingRepository.findById(id).orElseThrow().getImageUrls()).containsExactly("https://b");
    listingRepository.deleteById(id);
    listingRepository.flush();
    assertThat(entityManager.createNativeQuery("select count(*) from listing_images where listing_id = :id")
        .setParameter("id", id).getSingleResult()).isEqualTo(0L);
  }

  @Test
  void findBySchoolDomain_returnsMatchingSellersNewestFirst() {
    // Persist the seller's tenant separately: listings derive their domain from this profile.
    Student seller = studentRepository.save(new Student(
        "Sarah", "sarah@tulane.edu", "hashed", null, null));
    profileRepository.save(new Profile(seller, "Sarah", "CS", null, "tulane.edu"));

    Listing older = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"),
        Condition.GOOD, Category.FURNITURE, seller);
    older.setCreatedAt(Instant.now().minusSeconds(60));
    listingRepository.save(older);

    Listing newer = new Listing("Mini Fridge", "Barely used", new BigDecimal("40.00"),
        Condition.LIKE_NEW, Category.KITCHEN, seller);
    newer.setCreatedAt(Instant.now());
    listingRepository.save(newer);

    // A newer listing in another school must be filtered by the database, before sorting.
    Student other = studentRepository.save(new Student("Other", "other@school.edu", "hashed", null, null));
    profileRepository.save(new Profile(other, "Other", "CS", null, "school.edu"));
    listingRepository.save(new Listing("Excluded", "Other school", BigDecimal.TEN,
        Condition.GOOD, Category.FURNITURE, other));
    List<Listing> listings = listingRepository.findBySchoolDomain("tulane.edu");

    assertThat(listings).extracting(Listing::getItemName)
        .containsExactly("Mini Fridge", "Desk Lamp");
  }
}
