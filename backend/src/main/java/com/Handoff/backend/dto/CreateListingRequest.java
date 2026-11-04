package com.Handoff.backend.dto;

import java.math.BigDecimal;

public record CreateListingRequest(
    String itemName,
    String description,
    BigDecimal price,
    String condition,
    String category,
    String imageUrl) {
  public CreateListingRequest(String itemName, String description, BigDecimal price,
                              String condition, String category) {
    this(itemName, description, price, condition, category, null);
  }
}
