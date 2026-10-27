package com.Handoff.backend.dto;

import com.Handoff.backend.service.InvalidProfileSearchException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileSearchRequestTest {

  @Test
  void normalizesTheNameAndAppliesPaginationDefaults() {
    ProfileSearchRequest request = ProfileSearchRequest.parse("  JaNe  ", null, null);

    assertThat(request.query()).isEqualTo("jane");
    assertThat(request.page()).isEqualTo(1);
    assertThat(request.limit()).isEqualTo(20);
  }

  @Test
  void acceptsExplicitPagination() {
    ProfileSearchRequest request = ProfileSearchRequest.parse("ann", "2", "5");

    assertThat(request.page()).isEqualTo(2);
    assertThat(request.limit()).isEqualTo(5);
  }

  @Test
  void rejectsMissingBlankAndOversizedQueries() {
    assertThatThrownBy(() -> ProfileSearchRequest.parse(null, null, null))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("q is required");
    assertThatThrownBy(() -> ProfileSearchRequest.parse("   ", null, null))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("q is required");
    assertThatThrownBy(() -> ProfileSearchRequest.parse("a".repeat(101), null, null))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("q cannot exceed 100 characters");
  }

  @Test
  void rejectsMalformedPagination() {
    assertThatThrownBy(() -> ProfileSearchRequest.parse("jane", "first", null))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("page must be a valid integer");
    assertThatThrownBy(() -> ProfileSearchRequest.parse("jane", null, "many"))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("limit must be a valid integer");
  }

  @Test
  void rejectsPaginationOutsideThePublicContract() {
    assertThatThrownBy(() -> ProfileSearchRequest.parse("jane", "0", null))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("page must be at least 1");
    assertThatThrownBy(() -> ProfileSearchRequest.parse("jane", null, "0"))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("limit must be between 1 and 100");
    assertThatThrownBy(() -> ProfileSearchRequest.parse("jane", null, "101"))
        .isInstanceOf(InvalidProfileSearchException.class)
        .hasMessage("limit must be between 1 and 100");
  }
}
