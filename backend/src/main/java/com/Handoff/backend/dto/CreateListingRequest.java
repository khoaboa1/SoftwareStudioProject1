package com.Handoff.backend.dto;

import java.math.BigDecimal;

public record CreateListingRequest(
    String itemName,
    String description,
    BigDecimal price,
    String condition,
    String category) {
}
