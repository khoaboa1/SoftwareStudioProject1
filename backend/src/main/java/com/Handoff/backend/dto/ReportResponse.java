package com.Handoff.backend.dto;

import com.Handoff.backend.model.ListingReport;
import java.time.Instant;

public record ReportResponse(Long id, Long listingId, String reason, String details,
                             String status, Instant createdAt) {
  public static ReportResponse from(ListingReport report) {
    return new ReportResponse(report.getId(), report.getListing().getId(), report.getReason().name(),
        report.getDetails(), report.getStatus().name(), report.getCreatedAt());
  }
}
