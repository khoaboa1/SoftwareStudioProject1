package com.Handoff.backend.dto;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.service.InvalidListingException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused unit tests for {@link ListingBrowseRequest.parse}, covering the
 * parsing, normalization, and validation rules owned by SSP1-89.
 * No Spring context is required: parsing is a pure function of its inputs.
 */
class ListingBrowseRequestTest {

  @Test
  void defaultsWhenAllParametersOmitted() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        null, null, null, null, null, null, null, null);

    assertThat(request.keyword()).isNull();
    assertThat(request.category()).isNull();
    assertThat(request.minPrice()).isNull();
    assertThat(request.maxPrice()).isNull();
    assertThat(request.sortBy()).isEqualTo("createdAt");
    assertThat(request.sortDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(request.page()).isEqualTo(1);
    assertThat(request.limit()).isEqualTo(20);
  }

  @Test
  void keywordIsTrimmedAndLowercased() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        "  Desk Chair  ", null, null, null, null, null, null, null);

    assertThat(request.keyword()).isEqualTo("desk chair");
  }

  @Test
  void blankKeywordBecomesNull() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        "   ", null, null, null, null, null, null, null);

    assertThat(request.keyword()).isNull();
  }

  @Test
  void keywordOverMaxLengthIsRejected() {
    String tooLong = "a".repeat(201);

    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        tooLong, null, null, null, null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("q cannot exceed 200 characters");
  }

  @Test
  void keywordExactlyMaxLengthIsAccepted() {
    String exactly200 = "a".repeat(200);

    ListingBrowseRequest request = ListingBrowseRequest.parse(
        exactly200, null, null, null, null, null, null, null);

    assertThat(request.keyword()).isEqualTo(exactly200);
  }

  @Test
  void unsupportedCategoryIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, "books", null, null, null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("Unsupported category");
  }

  @Test
  void supportedCategoryParsesCaseInsensitively() {
    ListingBrowseRequest furniture = ListingBrowseRequest.parse(
        null, "furniture", null, null, null, null, null, null);
    assertThat(furniture.category()).isEqualTo(Category.FURNITURE);

    ListingBrowseRequest electronics = ListingBrowseRequest.parse(
        null, "ELECTRONICS", null, null, null, null, null, null);
    assertThat(electronics.category()).isEqualTo(Category.ELECTRONICS);
  }

  @Test
  void malformedMinPriceIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, "abc", null, null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("minPrice must be a valid number");
  }

  @Test
  void malformedMaxPriceIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, "xyz", null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("maxPrice must be a valid number");
  }

  @Test
  void negativeMinPriceIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, "-5.00", null, null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("minPrice cannot be negative");
  }

  @Test
  void negativeMaxPriceIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, "-5.00", null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("maxPrice cannot be negative");
  }

  @Test
  void minPriceGreaterThanMaxPriceIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, "20", "10", null, null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("minPrice cannot be greater than maxPrice");
  }

  @Test
  void minPriceEqualToMaxPriceIsAccepted() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        null, null, "10", "10", null, null, null, null);

    assertThat(request.minPrice()).isEqualByComparingTo(new BigDecimal("10"));
    assertThat(request.maxPrice()).isEqualByComparingTo(new BigDecimal("10"));
  }

  @Test
  void zeroPriceBoundsAreAcceptedAndWhitespaceIsTrimmed() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        null, null, " 0 ", " 0.00 ", null, null, null, null);

    assertThat(request.minPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(request.maxPrice()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void malformedPageIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, null, "abc", null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("page must be a valid integer");
  }

  @Test
  void pageLessThanOneIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, null, "0", null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("page must be at least 1");
  }

  @Test
  void integerOverflowIsRejectedAsMalformedPagination() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, null, "999999999999999999999", null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("page must be a valid integer");
  }

  @Test
  void malformedLimitIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, null, null, "xyz"))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("limit must be a valid integer");
  }

  @Test
  void limitOutOfBoundsIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, null, null, "0"))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("limit must be between 1 and 100");

    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, null, null, "101"))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("limit must be between 1 and 100");
  }

  @Test
  void paginationBoundsAndWhitespaceAreAccepted() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        null, null, null, null, null, null, " 2 ", " 100 ");

    assertThat(request.page()).isEqualTo(2);
    assertThat(request.limit()).isEqualTo(100);
  }

  @Test
  void unsupportedSortByIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, "seller", null, null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("sortBy must be one of: createdAt, price, itemName");
  }

  @Test
  void supportedSortFieldsParse() {
    ListingBrowseRequest createdAt = ListingBrowseRequest.parse(
        null, null, null, null, "createdAt", null, null, null);
    assertThat(createdAt.sortBy()).isEqualTo("createdAt");

    ListingBrowseRequest price = ListingBrowseRequest.parse(
        null, null, null, null, "price", null, null, null);
    assertThat(price.sortBy()).isEqualTo("price");

    ListingBrowseRequest itemName = ListingBrowseRequest.parse(
        null, null, null, null, "itemName", null, null, null);
    assertThat(itemName.sortBy()).isEqualTo("itemName");
  }

  @Test
  void unsupportedSortOrderIsRejected() {
    assertThatThrownBy(() -> ListingBrowseRequest.parse(
        null, null, null, null, null, "sideways", null, null))
        .isInstanceOf(InvalidListingException.class)
        .hasMessage("sortOrder must be asc or desc");
  }

  @Test
  void ascAndDescSortDirectionsParse() {
    ListingBrowseRequest asc = ListingBrowseRequest.parse(
        null, null, null, null, null, " ASC ", null, null);
    assertThat(asc.sortDirection()).isEqualTo(Sort.Direction.ASC);

    ListingBrowseRequest desc = ListingBrowseRequest.parse(
        null, null, null, null, null, "desc", null, null);
    assertThat(desc.sortDirection()).isEqualTo(Sort.Direction.DESC);

    ListingBrowseRequest defaultDir = ListingBrowseRequest.parse(
        null, null, null, null, null, null, null, null);
    assertThat(defaultDir.sortDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void blankOptionalValuesUseDefaults() {
    ListingBrowseRequest request = ListingBrowseRequest.parse(
        " ", " ", " ", " ", " ", " ", " ", " ");

    assertThat(request.keyword()).isNull();
    assertThat(request.category()).isNull();
    assertThat(request.minPrice()).isNull();
    assertThat(request.maxPrice()).isNull();
    assertThat(request.sortBy()).isEqualTo("createdAt");
    assertThat(request.sortDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(request.page()).isEqualTo(1);
    assertThat(request.limit()).isEqualTo(20);
  }
}
