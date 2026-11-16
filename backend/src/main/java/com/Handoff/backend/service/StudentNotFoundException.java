package com.Handoff.backend.service;

public class StudentNotFoundException extends RuntimeException {
  public StudentNotFoundException() { super("Student not found"); }
}
