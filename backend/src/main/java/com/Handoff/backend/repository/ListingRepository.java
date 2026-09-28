package com.Handoff.backend.repository;

import com.Handoff.backend.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
  /**
   * Match the seller's persisted profile by exact domain in the database.
   * EXISTS excludes sellers without profiles; fetching sellers also avoids per-item lookups
   * when building responses. No unscoped feed query is exposed here.
   */
  @Query("""
      select listing from Listing listing
      join fetch listing.seller seller
      where exists (
        select profile.id from Profile profile
        where profile.student = seller and profile.schoolDomain = :schoolDomain
      )
      order by listing.createdAt desc
      """)
  List<Listing> findBySchoolDomain(@Param("schoolDomain") String schoolDomain);
}
