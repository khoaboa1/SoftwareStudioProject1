package com.Handoff.backend.dto;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.service.InvalidListingException;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Validated, normalized search criteria for the listings browse endpoint.
 * Keeping parsing here prevents controller and repository layers from handling
 * untrusted strings independently.
 */
public record ListingBrowseRequest(
    String keyword,
    Category category,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String sortBy,
    Sort.Direction sortDirection,
    int page,
    int limit) {

  private static final int MAX_LIMIT = 100;
  private static final int MAX_KEYWORD_LENGTH = 200;
  // Map public API values to known entity properties; never pass arbitrary sort fields to JPA.
  private static final Map<String, String> SORT_FIELDS = Map.of(
      "createdAt", "createdAt",
      "price", "price",
      "itemName", "itemName");

  /** Apply endpoint defaults and reject malformed or unsupported query values. */
  public static ListingBrowseRequest parse(String q, String category, String minPrice,
                                           String maxPrice, String sortBy, String sortOrder,
                                           String page, String limit) {
    String keyword = normalizeKeyword(q);
    Category parsedCategory = parseCategory(category);
    BigDecimal parsedMinPrice = parsePrice("minPrice", minPrice);
    BigDecimal parsedMaxPrice = parsePrice("maxPrice", maxPrice);
    // Validate related fields after each value has been parsed independently.
    if (parsedMinPrice != null && parsedMaxPrice != null
        && parsedMinPrice.compareTo(parsedMaxPrice) > 0) {
      throw new InvalidListingException("minPrice cannot be greater than maxPrice");
    }

    String requestedSort = hasText(sortBy) ? sortBy.trim() : "createdAt";
    String parsedSort = SORT_FIELDS.get(requestedSort);
    if (parsedSort == null) {
      throw new InvalidListingException("sortBy must be one of: createdAt, price, itemName");
    }

    Sort.Direction direction = parseSortDirection(sortOrder);
    // The public API uses one-based pages; the service converts them for Spring Data.
    int parsedPage = parseInteger("page", page, 1);
    int parsedLimit = parseInteger("limit", limit, 20);
    if (parsedPage < 1) {
      throw new InvalidListingException("page must be at least 1");
    }
    if (parsedLimit < 1 || parsedLimit > MAX_LIMIT) {
      throw new InvalidListingException("limit must be between 1 and 100");
    }

    return new ListingBrowseRequest(keyword, parsedCategory, parsedMinPrice, parsedMaxPrice,
        parsedSort, direction, parsedPage, parsedLimit);
  }

  private static String normalizeKeyword(String raw) {
    if (!hasText(raw)) {
      return null;
    }
    String value = raw.trim();
    if (value.length() > MAX_KEYWORD_LENGTH) {
      throw new InvalidListingException("q cannot exceed 200 characters");
    }
    // Database predicates compare lower-case values for case-insensitive matching.
    return value.toLowerCase(Locale.ROOT);
  }

  private static Category parseCategory(String raw) {
    if (!hasText(raw)) {
      return null;
    }
    try {
      return Category.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new InvalidListingException("Unsupported category");
    }
  }

  private static BigDecimal parsePrice(String name, String raw) {
    if (!hasText(raw)) {
      return null;
    }
    try {
      BigDecimal value = new BigDecimal(raw.trim());
      if (value.signum() < 0) {
        throw new InvalidListingException(name + " cannot be negative");
      }
      return value;
    } catch (NumberFormatException exception) {
      throw new InvalidListingException(name + " must be a valid number");
    }
  }

  private static Sort.Direction parseSortDirection(String raw) {
    if (!hasText(raw) || "desc".equalsIgnoreCase(raw.trim())) {
      return Sort.Direction.DESC;
    }
    if ("asc".equalsIgnoreCase(raw.trim())) {
      return Sort.Direction.ASC;
    }
    throw new InvalidListingException("sortOrder must be asc or desc");
  }

  private static int parseInteger(String name, String raw, int defaultValue) {
    if (!hasText(raw)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException exception) {
      throw new InvalidListingException(name + " must be a valid integer");
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
