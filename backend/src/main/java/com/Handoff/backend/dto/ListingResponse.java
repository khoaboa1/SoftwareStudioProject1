package com.Handoff.backend.dto;

import com.Handoff.backend.model.Listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingResponse(
    Long id,
    String itemName,
    String description,
    BigDecimal price,
    String condition,
    String category,
    Long sellerId,
    String sellerName,
    Boolean sellerVerified,
    Instant createdAt,
    String imageUrl,
    List<String> imageUrls) {

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
        listing.getSeller().isVerified(),
        listing.getCreatedAt(),
        listing.getImageUrl(),
        listing.getImageUrls().isEmpty()
            ? (listing.getImageUrl() == null ? List.of() : List.of(listing.getImageUrl()))
            : listing.getImageUrls());
  }
}
