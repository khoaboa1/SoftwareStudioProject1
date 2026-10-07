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

public final class ListingSpecifications {

  private ListingSpecifications() {
  }

  public static Specification<Listing> visibleBrowseResults(String schoolDomain,
                                                             ListingBrowseRequest request) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

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

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
