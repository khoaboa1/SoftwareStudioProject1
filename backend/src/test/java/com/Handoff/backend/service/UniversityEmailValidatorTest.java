package com.Handoff.backend.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class UniversityEmailValidatorTest {

  private final UniversityEmailValidator validator = new UniversityEmailValidator();

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
  void isValid_acceptsExistingEduEmailForms(String email) {
    assertThat(validator.isValid(email)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
  void isValid_rejectsInvalidEmailWithoutNormalizing(String email) {
    assertThat(validator.isValid(email)).isFalse();
  }
}
