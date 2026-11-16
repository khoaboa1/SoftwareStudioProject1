package com.Handoff.backend.service;

public class DuplicateReportException extends RuntimeException {
  public DuplicateReportException() { super("You have already reported this listing"); }
}
