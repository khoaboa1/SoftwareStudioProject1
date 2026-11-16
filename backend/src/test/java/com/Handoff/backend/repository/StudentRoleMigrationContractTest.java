package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StudentRoleMigrationContractTest {
  @Test
  void v6AddsRoleAndSuspensionDefaults() throws Exception {
    ClassPathResource migration = new ClassPathResource("db/migration/V6__add_student_role_and_suspension.sql");
    assertThat(migration.exists()).isTrue();
    String sql = migration.getContentAsString(StandardCharsets.UTF_8)
        .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    assertThat(sql).contains("alter table students", "role varchar(20) not null default 'student'",
        "suspended boolean not null default false");
  }
}
