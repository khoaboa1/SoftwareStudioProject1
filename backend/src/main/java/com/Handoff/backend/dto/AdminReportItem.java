package com.Handoff.backend.dto;

import com.Handoff.backend.model.ListingReport;
import java.time.Instant;

public record AdminReportItem(Long id, String reason, String details, Instant createdAt, Long reporterId,
        String reporterName, Long listingId, String itemName, Long sellerId, String sellerName,
        boolean sellerSuspended) {
    public static AdminReportItem from(ListingReport report) {
        var listing = report.getListing();
        return new AdminReportItem(report.getId(), report.getReason().name(), report.getDetails(),
                report.getCreatedAt(), report.getReporter().getId(), report.getReporter().getStudentName(),
                listing.getId(), listing.getItemName(), listing.getSeller().getId(),
                listing.getSeller().getStudentName(), listing.getSeller().isSuspended());
    }
}
