package com.Handoff.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Red contract tests for the PostgreSQL index migration owned by SSP1-87.
 *
 * <p>The migration is intentionally absent on this test-only branch, so these
 * tests describe the required artifact and fail until SSP1-87 is implemented.
 * A later PostgreSQL smoke test must additionally execute the migration against
 * fresh and existing schemas; static SQL checks cannot prove database validity.</p>
 */
class ListingSearchIndexMigrationContractTest {

  private static final String MIGRATION_PATH =
      "db/migration/V2__add_listing_search_indexes.sql";

  @Test
  void versionedSearchIndexMigrationExists() {
    assertThat(new ClassPathResource(MIGRATION_PATH).exists())
        .as("SSP1-87 must provide %s", MIGRATION_PATH)
        .isTrue();
  }

  @Test
  void migrationCreatesIndexesForFilterSortAndJoinColumns() throws IOException {
    String sql = normalizedMigrationSql();

    assertThat(sql)
        .contains("create index if not exists idx_listings_status on listings (status)")
        .contains("create index if not exists idx_listings_category on listings (category)")
        .contains("create index if not exists idx_listings_price on listings (price)")
        .contains("create index if not exists idx_listings_created_at on listings (created_at)")
        .contains("create index if not exists idx_listings_seller_id on listings (seller_id)")
        .contains("create index if not exists idx_profiles_student_school_domain "
            + "on profiles (student_id, school_domain)");
  }

  @Test
  void migrationCreatesTrigramIndexesCompatibleWithLiteralSubstringSearch() throws IOException {
    String sql = normalizedMigrationSql();

    assertThat(sql)
        .contains("create extension if not exists pg_trgm")
        .contains("create index if not exists idx_listings_item_name_trgm on listings "
            + "using gin (lower(item_name) gin_trgm_ops)")
        .contains("create index if not exists idx_listings_description_trgm on listings "
            + "using gin (lower(description) gin_trgm_ops)");
  }

  @Test
  void migrationUsesPhysicalSnakeCaseColumnNames() throws IOException {
    String sql = normalizedMigrationSql();

    assertThat(sql)
        .contains("created_at")
        .contains("item_name")
        .contains("seller_id")
        .contains("student_id")
        .contains("school_domain")
        .doesNotContain("createdat")
        .doesNotContain("itemname")
        .doesNotContain("sellerid")
        .doesNotContain("studentid")
        .doesNotContain("schooldomain");
  }

  private String normalizedMigrationSql() throws IOException {
    ClassPathResource migration = new ClassPathResource(MIGRATION_PATH);
    assertThat(migration.exists())
        .as("SSP1-87 must provide %s before the migration contract can pass", MIGRATION_PATH)
        .isTrue();
    return migration.getContentAsString(StandardCharsets.UTF_8)
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .trim();
  }
}
