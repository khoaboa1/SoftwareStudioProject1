package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ListingImagesMigrationContractTest {
  @Test
  void v4CreatesListingImagesAndBackfills() throws Exception {
    ClassPathResource migration = new ClassPathResource("db/migration/V4__create_listing_images.sql");
    assertThat(migration.exists()).isTrue();
    String sql = migration.getContentAsString(StandardCharsets.UTF_8)
        .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    assertThat(sql).contains("create table", "listing_images", "references listings", "on delete cascade",
        "unique (listing_id, position)", "insert into listing_images", "from listings", "image_url is not null");
  }
}
