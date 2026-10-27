package com.Handoff.backend.dto;

import java.util.List;

/** Stable response envelope; callers do not depend on Spring Data's Page JSON. */
public record ProfileSearchPageResponse(List<ProfileSearchItem> items, Meta meta) {

  public record Meta(long totalItems, int currentPage, int totalPages, int pageSize) {
  }
}
