package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.Handoff.backend.model.Role;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RoleMigrationContractTest {
    @Test
    void v8AddsRoleColumnIdempotently() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V8__add_role_to_students.sql");
        assertThat(migration.exists()).isTrue();
        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertThat(sql).contains("alter table students");
        assertThat(sql).contains("add column if not exists role");
        assertThat(sql).contains("varchar(50)");
        assertThat(sql).contains("not null");
        assertThat(sql).contains("default 'student'");
    }

    @Test
    void v8CheckConstraintListsEveryRole() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V8__add_role_to_students.sql");
        assertThat(migration.exists()).isTrue();
        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        for (Role role : Role.values()) {
            assertThat(sql).contains("'" + role.name().toLowerCase(Locale.ROOT) + "'");
        }
    }
}
