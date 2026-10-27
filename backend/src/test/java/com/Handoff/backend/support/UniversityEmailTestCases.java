package com.Handoff.backend.support;

import java.util.stream.Stream;

/** Characterization cases shared by policy, service, and HTTP contract tests. */
public final class UniversityEmailTestCases {

  private UniversityEmailTestCases() {
  }

  public static Stream<String> acceptedEmails() {
    return Stream.of(
        "jane@tulane.edu",
        "Jane@TULANE.EDU",
        "jane@cs.tulane.edu",
        "jane+books@tulane.edu",
        // Preserve the existing policy's permissiveness rather than tightening the regex.
        "jane@tulane..edu");
  }

  public static Stream<String> rejectedEmails() {
    return Stream.of(
        null, "", " ",
        " jane@tulane.edu", "jane@tulane.edu ",
        "jane doe@tulane.edu", "jane@tu lane.edu",
        "jane\t@tulane.edu", "jane@tulane.edu\n",
        "@tulane.edu", "jane@", "jane@.edu",
        "janetulane.edu", "jane@@tulane.edu",
        "jane@gmail.com", "jane@tulane.edu.com");
  }
}
