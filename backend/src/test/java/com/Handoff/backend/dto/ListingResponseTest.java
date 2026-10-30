package com.Handoff.backend.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListingResponseTest {

  private ListingResponse listingResponse;
  private Listing listing;
  private Student student;

  @BeforeEach
  void setUp() {
    student = new Student();
    listing = new Listing();
    listing.setSeller(student);
    listing.setCondition(Condition.NEW);
    listing.setCategory(Category.FURNITURE);
    listingResponse = ListingResponse.from(listing);
  }

  @Test
  void from_verifiedSeller_setsSellerVerifiedTrue() {
    student.setVerified(true);
    listingResponse = ListingResponse.from(listing);
    assertTrue(listingResponse.sellerVerified(), "Expected sellerVerified to be true when seller is verified");
  }

  @Test
  void from_unverifiedSeller_setsSellerVerifiedFalse() {
    student.setVerified(false);
    listing = new Listing();
    listing.setSeller(student);
    listing.setCondition(Condition.NEW);
    listing.setCategory(Category.FURNITURE);
    listingResponse = ListingResponse.from(listing);
    assertFalse(listingResponse.sellerVerified(), "Expected sellerVerified to be false when seller is unverified");
  }
}
