package com.Handoff.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ConversationsMigrationContractTest {
  @Test
  void v5CreatesConversations() throws Exception {
    ClassPathResource migration = new ClassPathResource("db/migration/V5__create_conversations.sql");
    assertThat(migration.exists()).isTrue();
    String sql = migration.getContentAsString(StandardCharsets.UTF_8)
        .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    assertThat(sql).contains("create table", "conversations", "references listings", "on delete cascade",
        "references students", "unique (listing_id, buyer_id)");
  }
}
