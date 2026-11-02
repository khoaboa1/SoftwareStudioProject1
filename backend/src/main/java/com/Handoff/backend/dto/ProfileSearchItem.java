package com.Handoff.backend.dto;

import com.Handoff.backend.model.Profile;

/** Public fields needed to render one compact user-search result. */
public record ProfileSearchItem(
    Long id,
    Long studentId,
    String name,
    String major,
    String bio) {

  public static ProfileSearchItem from(Profile profile) {
    return new ProfileSearchItem(
        profile.getId(),
        profile.getStudentId(),
        profile.getName(),
        profile.getMajor(),
        profile.getBio());
  }
}
