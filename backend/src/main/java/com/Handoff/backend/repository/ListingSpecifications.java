package com.Handoff.backend.repository;

import com.Handoff.backend.dto.ListingBrowseRequest;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Profile;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Builds one database predicate from the optional SSP1-90 browse filters. */
public final class ListingSpecifications {

  private ListingSpecifications() {
  }

  /**
   * Combines tenant isolation, lifecycle visibility, keyword search, and
   * attribute filters with AND so the page query and count query stay aligned.
   */
  public static Specification<Listing> visibleBrowseResults(String schoolDomain,
                                                             ListingBrowseRequest request) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Derive tenancy from each seller's persisted profile; clients cannot override the domain.
      Subquery<Long> matchingProfile = query.subquery(Long.class);
      Root<Profile> profile = matchingProfile.from(Profile.class);
      matchingProfile.select(profile.get("id"));
      matchingProfile.where(
          criteriaBuilder.equal(profile.get("student"), root.get("seller")),
          criteriaBuilder.equal(profile.get("schoolDomain"), schoolDomain));
      predicates.add(criteriaBuilder.exists(matchingProfile));

      // Existing rows predate the status column; null is treated as ACTIVE during migration.
      predicates.add(criteriaBuilder.or(
          criteriaBuilder.equal(root.get("status"), ListingStatus.ACTIVE),
          criteriaBuilder.isNull(root.get("status"))));

      if (request.keyword() != null) {
        // Escape SQL LIKE metacharacters so user input is matched as literal text.
        String escaped = escapeLike(request.keyword());
        String pattern = "%" + escaped + "%";
        predicates.add(criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("itemName")), pattern, '\\'),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern, '\\')));
      }
      if (request.category() != null) {
        predicates.add(criteriaBuilder.equal(root.get("category"), request.category()));
      }
      if (request.minPrice() != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.minPrice()));
      }
      if (request.maxPrice() != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.maxPrice()));
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  /** Escape the configured LIKE escape character before percent and underscore. */
  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
