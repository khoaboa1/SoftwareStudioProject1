package com.Handoff.backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Student;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link ListingResponse#from(Listing)}.
 *
 * <p>These tests pin the public listing response shape: {@code sellerVerified} is
 * derived server-side from the seller, and no sensitive student account data ever
 * leaks into listing JSON.
 */
class ListingResponseContractTest {

  @Test
  void serialize_includesSellerVerifiedTrueForVerifiedSeller() {
    Student seller = new Student();
    seller.setVerified(true);

    Listing listing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"),
        Condition.GOOD, Category.FURNITURE, seller);

    ListingResponse response = ListingResponse.from(listing);

    assertTrue(response.sellerVerified());
  }

  @Test
  void serialize_includesSellerVerifiedFalseForUnverifiedSeller() {
    Student seller = new Student();
    seller.setVerified(false);

    Listing listing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"),
        Condition.GOOD, Category.FURNITURE, seller);

    ListingResponse response = ListingResponse.from(listing);

    assertFalse(response.sellerVerified());
  }

  @Test
  void serialize_excludesSensitiveStudentFields() throws Exception {
    // Populate every sensitive student field to prove it does not leak into the response.
    Student seller = new Student();
    seller.setId(42L);
    seller.setStudentName("Bob Smith");
    seller.setEmail("bob@tulane.edu");
    seller.setPasswordHash("sensitive-hash");
    seller.setOauthProvider("google");
    seller.setVerificationPin("123456");
    seller.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(30));
    seller.setTrustedDeviceIds(Set.of("device-1", "device-2"));

    Listing listing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"),
        Condition.GOOD, Category.FURNITURE, seller);

    ListingResponse response = ListingResponse.from(listing);

    // The record must carry only the 9 public listing fields; sellerVerified is the
    // only verification-related field and no sensitive student field is present.
    List<String> components = java.util.Arrays.stream(ListingResponse.class.getRecordComponents())
        .map(java.lang.reflect.RecordComponent::getName)
        .toList();

    assertEquals(10, components.size());
    assertTrue(components.contains("sellerVerified"));
    assertFalse(components.stream().anyMatch(name ->
        name.equals("passwordHash")
            || name.equals("verificationPin")
            || name.equals("verificationPinExpiry")
            || name.equals("trustedDeviceIds")
            || name.equals("oauthProvider")
            || name.equals("emailVerified")));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(response);

    assertFalse(json.contains("passwordHash"), "passwordHash must not appear in listing JSON");
    assertFalse(json.contains("verificationPin"), "verificationPin must not appear in listing JSON");
    assertFalse(json.contains("verificationPinExpiry"),
        "verificationPinExpiry must not appear in listing JSON");
    assertFalse(json.contains("trustedDeviceIds"),
        "trustedDeviceIds must not appear in listing JSON");
    assertFalse(json.contains("oauthProvider"), "oauthProvider must not appear in listing JSON");
    assertFalse(json.contains("email_verified"), "email_verified must not appear in listing JSON");

    assertTrue(json.contains("sellerVerified"), "sellerVerified must be present in listing JSON");
  }
}
