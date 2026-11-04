package com.Handoff.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateListingRequest(
    String itemName,
    String description,
    BigDecimal price,
    String condition,
    String category,
    String imageUrl,
    List<String> imageUrls) {
  public CreateListingRequest(String itemName, String description, BigDecimal price,
                              String condition, String category) {
    this(itemName, description, price, condition, category, null, null);
  }

  public CreateListingRequest(String itemName, String description, BigDecimal price,
                              String condition, String category, String imageUrl) {
    this(itemName, description, price, condition, category, imageUrl, null);
  }
}
