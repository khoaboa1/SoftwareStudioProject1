package com.Handoff.backend.dto;

import com.Handoff.backend.model.Listing;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingResponse(
    Long id,
    String itemName,
    String description,
    BigDecimal price,
    String condition,
    String category,
    Long sellerId,
    String sellerName,
    Instant createdAt) {

  public static ListingResponse from(Listing listing) {
    return new ListingResponse(
        listing.getId(),
        listing.getItemName(),
        listing.getDescription(),
        listing.getPrice(),
        listing.getCondition().name(),
        listing.getCategory().name(),
        listing.getSeller().getId(),
        listing.getSeller().getStudentName(),
        listing.getCreatedAt());
  }
}
