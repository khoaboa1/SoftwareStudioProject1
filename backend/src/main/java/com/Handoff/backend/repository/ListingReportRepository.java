package com.Handoff.backend.repository;

import com.Handoff.backend.model.ListingReport;
import com.Handoff.backend.model.ReportStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingReportRepository extends JpaRepository<ListingReport, Long> {
    boolean existsByListing_IdAndReporter_IdAndStatus(Long listingId, Long reporterId, ReportStatus status);

    @Query("""
            select report from ListingReport report
            join fetch report.listing listing
            join fetch listing.seller seller
            join fetch report.reporter
            where report.status = com.Handoff.backend.model.ReportStatus.OPEN
            and exists (select profile.id from Profile profile
              where profile.student = seller and profile.schoolDomain = :schoolDomain)
            order by report.createdAt desc, report.id desc
            """)
    List<ListingReport> findOpenForSchool(@Param("schoolDomain") String schoolDomain);

    @Query("""
            select report from ListingReport report
            join fetch report.listing listing
            join fetch listing.seller seller
            join fetch report.reporter
            where report.id = :id and exists (select profile.id from Profile profile
              where profile.student = seller and profile.schoolDomain = :schoolDomain)
            """)
    Optional<ListingReport> findByIdForSchool(@Param("id") Long id, @Param("schoolDomain") String schoolDomain);
}
