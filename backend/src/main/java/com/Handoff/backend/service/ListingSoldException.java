package com.Handoff.backend.service;

public class ListingSoldException extends RuntimeException {
  public ListingSoldException() {
    super("This item has already been sold");
  }
}
