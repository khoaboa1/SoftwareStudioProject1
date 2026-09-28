package com.Handoff.backend.controller;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SSP1-70 acceptance tests: exercise the HTTP endpoint through the real service and H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Roll back fixtures after each case so domains cannot leak between tests.
class ListingDomainScopingTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StudentRepository studentRepository;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private ListingRepository listingRepository;

  /** Persist a student and optional profile; the domain deliberately comes from the profile. */
  private Student student(String name, String domain) {
    Student student = studentRepository.save(new Student(name, name + "@example.edu", "hashed", null, null));
    if (domain != null) {
      profileRepository.save(new Profile(student, name, "Computer Science", null, domain));
    }
    return student;
  }

  /** Model the server-side studentId session established by the existing authentication flow. */
  private MockHttpSession session(Student student) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
    return session;
  }

  /** Give listings deterministic timestamps so ordering assertions do not depend on clock timing. */
  private void listing(Student seller, String name, long seconds) {
    Listing listing = new Listing(name, "Test item", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller);
    listing.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(seconds));
    listingRepository.save(listing);
  }

  @Test
  void feed_returnsOnlyExactProfileDomain_newestFirst() throws Exception {
    // Include the requester's listing, a peer's listing, another school, and near-matching domains.
    Student viewer = student("viewer", "tulane.edu");
    listing(viewer, "Older", 1);
    listing(student("peer", "tulane.edu"), "Newer", 2);
    listing(student("other", "other-school.edu"), "Other school", 3);
    listing(student("subdomain", "students.tulane.edu"), "Subdomain", 4);
    listing(student("suffix", "not-tulane.edu"), "Similar suffix", 5);
    listing(student("prefix", "tulane.edu.evil.edu"), "Similar prefix", 6);

    // Exact array contents prove both isolation and retained chronological ordering.
    mockMvc.perform(get("/listings").session(session(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].itemName").value("Newer"))
        .andExpect(jsonPath("$[1].itemName").value("Older"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"domain", "schoolDomain", "school_domain"})
  void feed_ignoresClientDomainOverrides(String parameter) throws Exception {
    // A forged tenant parameter must not substitute for the authenticated profile's domain.
    Student viewer = student("viewer", "tulane.edu");
    listing(student("peer", "tulane.edu"), "Allowed", 1);
    listing(student("other", "other-school.edu"), "Forbidden", 2);

    mockMvc.perform(get("/listings").session(session(viewer)).param(parameter, "other-school.edu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].itemName").value("Allowed"));
  }

  @Test
  void feed_scopesIndependentlyForEachStudent() throws Exception {
    // Consecutive requests must resolve each session's profile without retaining the first domain.
    Student first = student("first", "tulane.edu");
    Student second = student("second", "other-school.edu");
    listing(first, "Tulane", 1);
    listing(second, "Other school", 2);

    mockMvc.perform(get("/listings").session(session(first)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].itemName").value("Tulane"));
    mockMvc.perform(get("/listings").session(session(second)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].itemName").value("Other school"));
  }

  @Test
  void feed_withoutSession_returnsUnauthorized() throws Exception {
    // Supplying a domain does not authenticate an anonymous request.
    mockMvc.perform(get("/listings").param("domain", "tulane.edu"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").isString());
  }

  @Test
  void feed_withStaleSession_returnsUnauthorized() throws Exception {
    // A session referring to a deleted/nonexistent student must not grant feed access.
    MockHttpSession stale = new MockHttpSession();
    stale.setAttribute(SessionKeys.STUDENT_ID, Long.MAX_VALUE);
    mockMvc.perform(get("/listings").session(stale))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void feed_withoutRequesterProfile_returnsNotFound() throws Exception {
    // Never fall back to a request parameter or an unscoped query when a profile is missing.
    Student viewer = student("viewer", null);
    listing(student("other", "other-school.edu"), "Forbidden", 1);
    mockMvc.perform(get("/listings").session(session(viewer)).param("domain", "other-school.edu"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Profile not found"));
  }

  @Test
  void feed_excludesSellersWithoutProfiles() throws Exception {
    // Legacy listings remain hidden until their sellers have a trustworthy tenant identifier.
    Student viewer = student("viewer", "tulane.edu");
    listing(student("legacy", null), "Unscoped", 2);
    listing(student("peer", "tulane.edu"), "Allowed", 1);
    mockMvc.perform(get("/listings").session(session(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].itemName").value("Allowed"));
  }

  @Test
  void feed_withoutMatchingListings_returnsEmptyArray() throws Exception {
    // Other schools' listings must not populate an otherwise empty school marketplace.
    Student viewer = student("viewer", "tulane.edu");
    listing(student("other", "other-school.edu"), "Forbidden", 1);
    mockMvc.perform(get("/listings").session(session(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
