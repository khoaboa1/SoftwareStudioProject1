package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ListingReportsMigrationContractTest {
    @Test
    void v10CreatesReportsWithCascadeIndexAndRowLevelSecurity() throws Exception {
        var resource = new ClassPathResource("db/migration/V10__create_listing_reports.sql");
        assertThat(resource.exists()).isTrue();
        String sql = resource.getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT).replaceAll("\\s+",
                " ");
        assertThat(sql).contains("create table listing_reports", "references listings", "on delete cascade",
                "references students", "status varchar(20) not null default 'open'", "created_at",
                "(status, listing_id)", "alter table listing_reports enable row level security");
    }
}
