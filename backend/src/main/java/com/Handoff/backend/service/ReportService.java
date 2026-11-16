package com.Handoff.backend.service;

import com.Handoff.backend.dto.CreateReportRequest;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingReport;
import com.Handoff.backend.model.ReportReason;
import com.Handoff.backend.model.ReportStatus;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingReportRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ListingService listingService;
    private final ListingReportRepository reports;

    public ReportService(ListingService listingService, ListingReportRepository reports) {
        this.listingService = listingService;
        this.reports = reports;
    }

    public ListingReport report(Long listingId, Student reporter, CreateReportRequest request) {
        ReportReason reason;
        try {
            reason = ReportReason.valueOf(request.reason());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidReportException("reason must be one of SCAM, PROHIBITED_ITEM, SPAM, OFFENSIVE, OTHER");
        }
        String details = request.details() == null ? null : request.details().trim();
        if (details != null && details.length() > 500) {
            throw new InvalidReportException("details cannot exceed 500 characters");
        }
        if (details != null && details.isBlank())
            details = null;
        Listing listing = listingService.getListing(listingId, reporter);
        if (listing.getSeller().getId().equals(reporter.getId()))
            throw new OwnListingReportException();
        if (reports.existsByListing_IdAndReporter_IdAndStatus(listingId, reporter.getId(), ReportStatus.OPEN)) {
            throw new DuplicateReportException();
        }
        return reports.save(new ListingReport(listing, reporter, reason, details));
    }
}
