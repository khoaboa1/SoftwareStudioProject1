package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateReportRequest;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.ReportResponse;
import com.Handoff.backend.service.DuplicateReportException;
import com.Handoff.backend.service.InvalidReportException;
import com.Handoff.backend.service.ListingNotFoundException;
import com.Handoff.backend.service.OwnListingReportException;
import com.Handoff.backend.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
public class ReportController {
  private final ReportService reportService;
  private final CurrentStudentResolver resolver;
  public ReportController(ReportService reportService, CurrentStudentResolver resolver) {
    this.reportService = reportService;
    this.resolver = resolver;
  }
  @PostMapping("/{id}/reports")
  public ResponseEntity<ReportResponse> report(@PathVariable Long id, @RequestBody CreateReportRequest body,
                                                HttpServletRequest request) {
    var student = resolver.require(request, "You must be logged in to report a listing");
    return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(reportService.report(id, student, body)));
  }
  @ExceptionHandler(InvalidReportException.class)
  public ResponseEntity<ErrorResponse> invalid(InvalidReportException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }
  @ExceptionHandler(OwnListingReportException.class)
  public ResponseEntity<ErrorResponse> own(OwnListingReportException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
  }
  @ExceptionHandler(DuplicateReportException.class)
  public ResponseEntity<ErrorResponse> duplicate(DuplicateReportException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
  }
  @ExceptionHandler(ListingNotFoundException.class)
  public ResponseEntity<ErrorResponse> missing(ListingNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }
}
