package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ListingImageUrlMigrationContractTest {
  @Test
  void v3AddsNullableImageUrlColumn() throws Exception {
    ClassPathResource migration = new ClassPathResource("db/migration/V3__add_listing_image_url.sql");
    assertThat(migration.exists()).isTrue();
    String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    assertThat(sql).contains("alter table listings", "image_url", "varchar(1000)")
        .doesNotContain("not null");
  }
}
