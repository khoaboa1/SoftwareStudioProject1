package com.Handoff.backend.controller;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises SSP1-90 through HTTP, the real service, and the test database so
 * response metadata and database filtering are verified together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListingBrowseControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StudentRepository studentRepository;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private ListingRepository listingRepository;

  @Test
  void browse_defaultsToNewestFirstPaginationAndVisibleActiveListings() throws Exception {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Student otherSchool = student("Other", "other@loyola.edu", "loyola.edu");

    listing(seller, "Old", "active", "10.00", Category.FURNITURE, ListingStatus.ACTIVE, 1);
    listing(seller, "Middle", "legacy", "20.00", Category.KITCHEN, null, 2);
    listing(seller, "Newest", "active", "30.00", Category.ELECTRONICS, ListingStatus.ACTIVE, 3);
    listing(seller, "Draft", "hidden", "40.00", Category.FURNITURE, ListingStatus.DRAFT, 4);
    listing(seller, "Inactive", "hidden", "50.00", Category.FURNITURE, ListingStatus.INACTIVE, 5);
    listing(seller, "Deleted", "hidden", "60.00", Category.FURNITURE, ListingStatus.DELETED, 6);
    listing(otherSchool, "Other school", "hidden", "70.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 7);

    mockMvc.perform(get("/api/listings").session(session(viewer)).param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].itemName").value("Newest"))
        .andExpect(jsonPath("$.items[1].itemName").value("Middle"))
        .andExpect(jsonPath("$.meta.totalItems").value(3))
        .andExpect(jsonPath("$.meta.currentPage").value(1))
        .andExpect(jsonPath("$.meta.totalPages").value(2))
        .andExpect(jsonPath("$.meta.pageSize").value(2));
  }

  @Test
  void browse_combinesKeywordCategoryPriceAndSortFilters() throws Exception {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listing(seller, "Desk Chair", "Comfortable", "45.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 1);
    listing(seller, "Chair Mat", "Protects floors", "25.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 2);
    listing(seller, "Kitchen Chair", "Too expensive", "125.00", Category.FURNITURE,
        ListingStatus.ACTIVE, 3);
    listing(seller, "Desk Chair", "Wrong category", "30.00", Category.OTHER,
        ListingStatus.ACTIVE, 4);

    mockMvc.perform(get("/api/listings").session(session(viewer))
            .param("q", "  CHAIR ")
            .param("category", "furniture")
            .param("minPrice", "20")
            .param("maxPrice", "50")
            .param("sortBy", "price")
            .param("sortOrder", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].itemName").value("Chair Mat"))
        .andExpect(jsonPath("$.items[0].price").value(25.00))
        .andExpect(jsonPath("$.items[1].itemName").value("Desk Chair"))
        .andExpect(jsonPath("$.meta.totalItems").value(2));
  }

  @Test
  void browse_treatsSqlWildcardCharactersAsLiteralKeywordText() throws Exception {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listing(seller, "100% Cotton", "Literal percent", "10.00", Category.CLOTHING,
        ListingStatus.ACTIVE, 1);
    listing(seller, "Ordinary Shirt", "No wildcard", "12.00", Category.CLOTHING,
        ListingStatus.ACTIVE, 2);

    mockMvc.perform(get("/api/listings").session(session(viewer)).param("q", "%"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].itemName").value("100% Cotton"));
  }

  @Test
  void browse_outOfRangePageReturnsEmptyItemsWithActualTotals() throws Exception {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    listing(seller, "Desk", "Item", "10.00", Category.FURNITURE, ListingStatus.ACTIVE, 1);

    mockMvc.perform(get("/api/listings").session(session(viewer))
            .param("page", "5").param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)))
        .andExpect(jsonPath("$.meta.totalItems").value(1))
        .andExpect(jsonPath("$.meta.currentPage").value(5))
        .andExpect(jsonPath("$.meta.totalPages").value(1));
  }

  @Test
  void browse_requiresAuthenticationAndProfile() throws Exception {
    mockMvc.perform(get("/api/listings"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("You must be logged in to view marketplace listings"));

    Student viewer = studentRepository.save(
        new Student("Viewer", "viewer@tulane.edu", "hashed", null, null));
    mockMvc.perform(get("/api/listings").session(session(viewer)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Profile not found"));
  }

  @Test
  void browse_rejectsInvalidParametersWithClearMessages() throws Exception {
    Student viewer = student("Viewer", "viewer@tulane.edu", "tulane.edu");

    assertBadRequest(viewer, "page", "0", "page must be at least 1");
    assertBadRequest(viewer, "limit", "101", "limit must be between 1 and 100");
    assertBadRequest(viewer, "minPrice", "abc", "minPrice must be a valid number");
    assertBadRequest(viewer, "minPrice", "20", "maxPrice", "10",
        "minPrice cannot be greater than maxPrice");
    assertBadRequest(viewer, "category", "books", "Unsupported category");
    assertBadRequest(viewer, "sortBy", "seller", "sortBy must be one of: createdAt, price, itemName");
    assertBadRequest(viewer, "sortOrder", "sideways", "sortOrder must be asc or desc");
  }

  private void assertBadRequest(Student viewer, String parameter, String value, String message)
      throws Exception {
    mockMvc.perform(get("/api/listings").session(session(viewer)).param(parameter, value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(message));
  }

  private void assertBadRequest(Student viewer, String firstParameter, String firstValue,
                                String secondParameter, String secondValue, String message)
      throws Exception {
    mockMvc.perform(get("/api/listings").session(session(viewer))
            .param(firstParameter, firstValue).param(secondParameter, secondValue))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(message));
  }

  private Student student(String name, String email, String domain) {
    // Persist the tenant separately because production scoping reads Profile.schoolDomain.
    Student student = studentRepository.save(new Student(name, email, "hashed", null, null));
    profileRepository.save(new Profile(student, name, "Computer Science", null, domain));
    return student;
  }

  private MockHttpSession session(Student student) {
    // Match the server-side session established by the existing authentication flow.
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
    return session;
  }

  private Listing listing(Student seller, String name, String description, String price,
                          Category category, ListingStatus status, long seconds) {
    // Fixed timestamps make pagination and ordering assertions deterministic.
    Listing listing = new Listing(name, description, new BigDecimal(price),
        Condition.GOOD, category, seller);
    listing.setStatus(status);
    listing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(seconds));
    return listingRepository.save(listing);
  }
}
