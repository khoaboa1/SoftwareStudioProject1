package com.Handoff.backend.dto;

import com.Handoff.backend.service.InvalidProfileSearchException;

import java.util.Locale;

/**
 * Validated, normalized query parameters for the profile-name search exercise.
 *
 * <p>The public API uses one-based page numbers. Keeping parsing here means the
 * controller and repository never have to interpret raw, untrusted strings.</p>
 */
public record ProfileSearchRequest(String query, int page, int limit) {

  private static final int DEFAULT_PAGE = 1;
  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 100;
  private static final int MAX_QUERY_LENGTH = 100;

  public static ProfileSearchRequest parse(String q, String page, String limit) {
    String normalizedQuery = normalizeQuery(q);
    int parsedPage = parseInteger("page", page, DEFAULT_PAGE);
    int parsedLimit = parseInteger("limit", limit, DEFAULT_LIMIT);
    validatePagination(parsedPage, parsedLimit);
    return new ProfileSearchRequest(normalizedQuery, parsedPage, parsedLimit);
  }

  private static String normalizeQuery(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new InvalidProfileSearchException("q is required");
    }
    String normalized = raw.trim();
    if (normalized.length() > MAX_QUERY_LENGTH) {
      throw new InvalidProfileSearchException("q cannot exceed 100 characters");
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  private static int parseInteger(String name, String raw, int defaultValue) {
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException exception) {
      throw new InvalidProfileSearchException(name + " must be a valid integer");
    }
  }

  private static void validatePagination(int page, int limit) {
    if (page < 1) {
      throw new InvalidProfileSearchException("page must be at least 1");
    }
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new InvalidProfileSearchException("limit must be between 1 and 100");
    }
  }
}
