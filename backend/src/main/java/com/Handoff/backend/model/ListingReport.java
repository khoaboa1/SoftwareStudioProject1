package com.Handoff.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "listing_reports")
public class ListingReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Listing listing;
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Student reporter;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;
    @Column(length = 500)
    private String details;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ListingReport() {
    }

    public ListingReport(Listing listing, Student reporter, ReportReason reason, String details) {
        this.listing = listing;
        this.reporter = reporter;
        this.reason = reason;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public Student getReporter() {
        return reporter;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void dismiss() {
        status = ReportStatus.DISMISSED;
    }
}
