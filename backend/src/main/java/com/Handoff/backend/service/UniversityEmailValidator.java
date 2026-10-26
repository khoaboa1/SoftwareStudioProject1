package com.Handoff.backend.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Shared university email policy; checks the original input without normalizing it. */
@Component
public class UniversityEmailValidator {

  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  public boolean isValid(String email) {
    return email != null && EDU_EMAIL_PATTERN.matcher(email).matches();
  }
}
