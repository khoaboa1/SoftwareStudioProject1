package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StudentSuspensionMigrationContractTest {
  @Test
  void v9AddsSuspensionDefault() throws Exception {
    ClassPathResource migration = new ClassPathResource("db/migration/V9__add_student_suspension.sql");
    assertThat(migration.exists()).isTrue();
    String sql = migration.getContentAsString(StandardCharsets.UTF_8)
        .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    assertThat(sql).contains("alter table students", "suspended boolean not null default false");
    // The role column comes from V8 (SSP1-114); V9 must not add it again.
    assertThat(sql).doesNotContain("add column role", "add column if not exists role");
  }
}
