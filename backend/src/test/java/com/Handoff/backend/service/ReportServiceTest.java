package com.Handoff.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.Handoff.backend.dto.CreateReportRequest;
import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingReport;
import com.Handoff.backend.model.ReportReason;
import com.Handoff.backend.model.ReportStatus;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingReportRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
  @Mock ListingService listings;
  @Mock ListingReportRepository reports;
  private ReportService service;
  private Student reporter;
  private Listing listing;

  @BeforeEach void setUp() {
    service = new ReportService(listings, reports);
    reporter = new Student("Reporter", "reporter@tulane.edu", "hash", null, null);
    reporter.setId(1L);
    Student seller = new Student("Seller", "seller@tulane.edu", "hash", null, null);
    seller.setId(2L);
    listing = new Listing("Lamp", "Works", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller);
    listing.setId(3L);
  }
  @Test void report_valid_savesOpenReport() {
    when(listings.getListing(3L, reporter)).thenReturn(listing);
    when(reports.save(any(ListingReport.class))).thenAnswer(call -> call.getArgument(0));
    ListingReport result = service.report(3L, reporter, new CreateReportRequest("SCAM", " suspicious "));
    assertThat(result.getReason()).isEqualTo(ReportReason.SCAM);
    assertThat(result.getStatus()).isEqualTo(ReportStatus.OPEN);
    assertThat(result.getDetails()).isEqualTo("suspicious");
  }
  @Test void report_ownListing_forbidden() {
    listing.setSeller(reporter);
    when(listings.getListing(3L, reporter)).thenReturn(listing);
    assertThatThrownBy(() -> service.report(3L, reporter, new CreateReportRequest("SPAM", null)))
        .isInstanceOf(OwnListingReportException.class).hasMessage("You cannot report your own listing");
  }
  @Test void report_duplicateOpen_conflict() {
    when(listings.getListing(3L, reporter)).thenReturn(listing);
    when(reports.existsByListing_IdAndReporter_IdAndStatus(3L, 1L, ReportStatus.OPEN)).thenReturn(true);
    assertThatThrownBy(() -> service.report(3L, reporter, new CreateReportRequest("SPAM", null)))
        .isInstanceOf(DuplicateReportException.class).hasMessage("You have already reported this listing");
  }
  @Test void report_afterDismissed_allowed() {
    when(listings.getListing(3L, reporter)).thenReturn(listing);
    when(reports.save(any(ListingReport.class))).thenAnswer(call -> call.getArgument(0));
    assertThat(service.report(3L, reporter, new CreateReportRequest("OTHER", null)).getStatus())
        .isEqualTo(ReportStatus.OPEN);
  }
  @Test void report_badReason_400() {
    assertThatThrownBy(() -> service.report(3L, reporter, new CreateReportRequest("scam", null)))
        .isInstanceOf(InvalidReportException.class).hasMessage("reason must be one of SCAM, PROHIBITED_ITEM, SPAM, OFFENSIVE, OTHER");
  }
  @Test void report_nullReason_400() {
    assertThatThrownBy(() -> service.report(3L, reporter, new CreateReportRequest(null, null)))
        .isInstanceOf(InvalidReportException.class);
  }
  @Test void report_detailsTooLong_400() {
    assertThatThrownBy(() -> service.report(3L, reporter, new CreateReportRequest("SCAM", "x".repeat(501))))
        .isInstanceOf(InvalidReportException.class).hasMessage("details cannot exceed 500 characters");
  }
  @Test void report_blankDetails_storedAsNull() {
    when(listings.getListing(3L, reporter)).thenReturn(listing);
    when(reports.save(any(ListingReport.class))).thenAnswer(call -> call.getArgument(0));
    assertThat(service.report(3L, reporter, new CreateReportRequest("SCAM", "  ")).getDetails()).isNull();
  }
  @Test void report_invisibleListing_propagatesNotFound() {
    when(listings.getListing(3L, reporter)).thenThrow(new ListingNotFoundException());
    assertThatThrownBy(() -> service.report(3L, reporter, new CreateReportRequest("SCAM", null)))
        .isInstanceOf(ListingNotFoundException.class);
  }
}
