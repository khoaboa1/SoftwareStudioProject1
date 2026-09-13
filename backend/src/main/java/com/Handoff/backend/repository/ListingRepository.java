package com.Handoff.backend.repository;

import com.Handoff.backend.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
  List<Listing> findAllByOrderByCreatedAtDesc();
}
