package com.Handoff.backend.dto;

import java.util.List;

/** Stable API envelope that avoids exposing Spring Data's internal Page JSON format. */
public record ListingPageResponse(List<ListingResponse> items, Meta meta) {

  /** Pagination values consumed by frontend navigation controls. */
  public record Meta(long totalItems, int currentPage, int totalPages, int pageSize) {
  }
}
