package com.Handoff.backend.service;

public class ListingNotFoundException extends RuntimeException {
  public ListingNotFoundException() {
    super("Listing not found");
  }
}
