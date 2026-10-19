package com.Handoff.backend.repository;

import com.Handoff.backend.dto.ListingBrowseRequest;
import com.Handoff.backend.dto.ListingPageResponse;
import com.Handoff.backend.dto.ListingResponse;
import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.service.ListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the dynamic repository/service query behavior owned by SSP1-88.
 * These tests deliberately bypass the HTTP controller and parameter-validation concerns.
 */
@SpringBootTest
@Transactional
class ListingBrowseQueryTest {

  @Autowired private ListingService listingService;
  @Autowired private ListingRepository listingRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private ProfileRepository profileRepository;

  @Test
  void keywordMatchesItemNameAndDescriptionCaseInsensitively() {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listing(seller, "Desk CHAIR", "Furniture", "25.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 1);
    listing(seller, "Floor Mat", "Fits beneath a chair", "15.00", Category.DECOR,
        ListingStatus.ACTIVE, 2);
    listing(seller, "Desk Lamp", "Adjustable light", "10.00", Category.ELECTRONICS,
        ListingStatus.ACTIVE, 3);

    ListingPageResponse result = browse(viewer, " ChAiR ", null, null, null,
        "createdAt", "asc", "1", "20");

    assertThat(names(result)).containsExactly("Desk CHAIR", "Floor Mat");
  }

  @Test
  void sqlWildcardCharactersAreMatchedLiterally() {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listing(seller, "100% Cotton", "Percent", "10.00", Category.CLOTHING,
        ListingStatus.ACTIVE, 1);
    listing(seller, "Under_score", "Underscore", "11.00", Category.OTHER,
        ListingStatus.ACTIVE, 2);
    listing(seller, "Ordinary", "No wildcard characters", "12.00", Category.OTHER,
        ListingStatus.ACTIVE, 3);

    assertThat(names(browse(viewer, "%", null, null, null,
        null, null, null, null))).containsExactly("100% Cotton");
    assertThat(names(browse(viewer, "_", null, null, null,
        null, null, null, null))).containsExactly("Under_score");
  }

  @Test
  void filtersCombineWithAndAndVisibilityRulesRemainEnforced() {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Student otherSchool = student("Other", "other@loyola.edu", "loyola.edu");
    Student profileless = student("Profileless", "none@tulane.edu", null);

    listing(seller, "Desk Chair", "Match active", "40.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 1);
    listing(seller, "Chair Mat", "Match legacy", "25.00", Category.FURNITURE,
        null, 2);
    listing(seller, "Desk Chair", "Wrong category", "30.00", Category.OTHER,
        ListingStatus.ACTIVE, 3);
    listing(seller, "Desk Chair", "Too expensive", "80.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 4);
    listing(seller, "Desk Chair", "Hidden draft", "35.00", Category.FURNITURE,
        ListingStatus.DRAFT, 5);
    listing(otherSchool, "Desk Chair", "Wrong school", "35.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 6);
    listing(profileless, "Desk Chair", "No seller profile", "35.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 7);

    ListingPageResponse result = browse(viewer, "chair", "furniture", "20", "50",
        "price", "asc", "1", "20");

    assertThat(names(result)).containsExactly("Chair Mat", "Desk Chair");
    assertThat(result.meta().totalItems()).isEqualTo(2);
  }

  @Test
  void omittedFiltersDefaultToNewestFirstWithDeterministicIdTieBreak() {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Instant sharedNewestTime = Instant.parse("2026-01-02T00:00:00Z");

    listingAt(seller, "Older", "10.00", sharedNewestTime.minusSeconds(1));
    listingAt(seller, "Newest Lower Id", "20.00", sharedNewestTime);
    listingAt(seller, "Newest Higher Id", "30.00", sharedNewestTime);

    ListingPageResponse result = browse(viewer, null, null, null, null,
        null, null, null, null);

    assertThat(names(result)).containsExactly(
        "Newest Higher Id", "Newest Lower Id", "Older");
  }

  @Test
  void everySupportedSortFieldWorksInBothDirections() {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listingAt(seller, "Beta", "30.00", Instant.parse("2026-01-01T00:00:01Z"));
    listingAt(seller, "Alpha", "10.00", Instant.parse("2026-01-01T00:00:03Z"));
    listingAt(seller, "Gamma", "20.00", Instant.parse("2026-01-01T00:00:02Z"));

    assertThat(names(browse(viewer, null, null, null, null,
        "itemName", "asc", null, null))).containsExactly("Alpha", "Beta", "Gamma");
    assertThat(names(browse(viewer, null, null, null, null,
        "itemName", "desc", null, null))).containsExactly("Gamma", "Beta", "Alpha");
    assertThat(names(browse(viewer, null, null, null, null,
        "price", "asc", null, null))).containsExactly("Alpha", "Gamma", "Beta");
    assertThat(names(browse(viewer, null, null, null, null,
        "price", "desc", null, null))).containsExactly("Beta", "Gamma", "Alpha");
    assertThat(names(browse(viewer, null, null, null, null,
        "createdAt", "asc", null, null))).containsExactly("Beta", "Gamma", "Alpha");
    assertThat(names(browse(viewer, null, null, null, null,
        "createdAt", "desc", null, null))).containsExactly("Alpha", "Gamma", "Beta");
  }

  @Test
  void paginationMetadataReflectsTheFilteredQuery() {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listing(seller, "Chair One", "Match", "10.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 1);
    listing(seller, "Chair Two", "Match", "20.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 2);
    listing(seller, "Chair Three", "Match", "30.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 3);
    listing(seller, "Lamp", "Excluded", "40.00", Category.ELECTRONICS,
        ListingStatus.ACTIVE, 4);

    ListingPageResponse result = browse(viewer, "chair", null, null, null,
        "createdAt", "asc", "2", "2");

    assertThat(names(result)).containsExactly("Chair Three");
    assertThat(result.meta().totalItems()).isEqualTo(3);
    assertThat(result.meta().currentPage()).isEqualTo(2);
    assertThat(result.meta().totalPages()).isEqualTo(2);
    assertThat(result.meta().pageSize()).isEqualTo(2);
  }

  private ListingPageResponse browse(Student viewer, String q, String category,
                                     String minPrice, String maxPrice, String sortBy,
                                     String sortOrder, String page, String limit) {
    return listingService.browseListings(viewer, ListingBrowseRequest.parse(
        q, category, minPrice, maxPrice, sortBy, sortOrder, page, limit));
  }

  private List<String> names(ListingPageResponse response) {
    return response.items().stream().map(ListingResponse::itemName).toList();
  }

  private Student student(String name, String email, String domain) {
    Student student = studentRepository.save(new Student(name, email, "hashed", null, null));
    if (domain != null) {
      profileRepository.save(new Profile(student, name, "Computer Science", null, domain));
    }
    return student;
  }

  private Listing listing(Student seller, String name, String description, String price,
                          Category category, ListingStatus status, long seconds) {
    Listing listing = new Listing(name, description, new BigDecimal(price),
        Condition.GOOD, category, seller);
    listing.setStatus(status);
    listing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(seconds));
    return listingRepository.save(listing);
  }

  private Listing listingAt(Student seller, String name, String price, Instant createdAt) {
    Listing listing = new Listing(name, "Description", new BigDecimal(price),
        Condition.GOOD, Category.FURNITURE, seller);
    listing.setCreatedAt(createdAt);
    return listingRepository.save(listing);
  }
}
