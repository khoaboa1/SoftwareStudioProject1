package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PinLimitMigrationContractTest {
    @Test
    void v12AddsThePinLimitColumnsIdempotently() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V12__add_pin_limits_to_students.sql");
        assertThat(migration.exists()).isTrue();
        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertThat(sql).contains("alter table students");
        assertThat(sql).contains("add column if not exists pin_failed_attempts integer not null default 0");
        assertThat(sql).contains("add column if not exists pin_last_sent_at timestamp");
        assertThat(sql).contains("add column if not exists pin_send_window_start timestamp");
        assertThat(sql).contains("add column if not exists pin_send_count integer not null default 0");
    }
}
