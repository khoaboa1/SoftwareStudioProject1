package com.Handoff.backend.dto;

import java.util.List;

public record ListingPageResponse(List<ListingResponse> items, Meta meta) {

  public record Meta(long totalItems, int currentPage, int totalPages, int pageSize) {
  }
}
