package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class MessagesMigrationContractTest {
    @Test
    void v11CreatesMessages() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V11__create_messages.sql");
        assertThat(migration.exists()).isTrue();
        String sql = migration.getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT).replaceAll("\\s+",
                " ");
        assertThat(sql).contains("create table", "messages", "references conversations", "references students",
                "on delete cascade", "varchar(2000)", "enable row level security");
    }
}
