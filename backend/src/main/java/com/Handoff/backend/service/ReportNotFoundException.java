package com.Handoff.backend.service;

public class ReportNotFoundException extends RuntimeException {
  public ReportNotFoundException() { super("Report not found"); }
}
