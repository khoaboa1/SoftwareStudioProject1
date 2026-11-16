package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateListingRequest;
import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.support.FakeSupabaseAuthClient;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ProfileRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FakeSupabaseAuthClient supabase;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        listingRepository.deleteAll();
        // Profiles reference students, so remove them before resetting student fixtures.
        profileRepository.deleteAll();
        studentRepository.deleteAll();
        supabase.reset();
    }

    private Cookie loginAsNewStudent(String email) throws Exception {
        String signupBody = objectMapper.writeValueAsString(new SignupRequest("Jane Doe", email, "Password123!"));
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
                .andExpect(status().isCreated());

        String pin = studentRepository.findByEmail(email).orElseThrow().getVerificationPin();
        VerificationRequest verifyBody = new VerificationRequest(email, pin, "test-device");
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(verifyBody))).andExpect(status().isOk());

        MvcResult loginResult = mockMvc
                .perform(post("/auth/login").contentType("application/json").content(
                        objectMapper.writeValueAsString(new LoginRequest(email, "Password123!", "test-device"))))
                .andExpect(status().isOk()).andReturn();

        return loginResult.getResponse().getCookie(AuthCookies.ACCESS);
    }

    @Test
    void createListing_thenFeed_returnsItNewestFirst() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");

        // Reading the feed now requires a profile with a server-owned school domain.
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));

        String body = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));

        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.itemName").value("Desk Lamp"))
                .andExpect(jsonPath("$.sellerName").value("Jane Doe"));

        mockMvc.perform(get("/listings").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("Desk Lamp"));
    }

    @Test
    void createListing_rejectsWhenNotLoggedIn() throws Exception {
        String body = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));

        mockMvc.perform(post("/listings").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListing_rejectsInvalidCondition() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");

        String body = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "SHINY", "FURNITURE"));

        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createListing_rejectsNonPositivePrice() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");

        String body = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("0"), "GOOD", "FURNITURE"));

        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateListing_owner_updatesFields() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        MvcResult createResult = mockMvc
                .perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

        mockMvc.perform(
                put("/listings/" + listingId).cookie(session).contentType("application/json").content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itemName").value("Mini Fridge"))
                .andExpect(jsonPath("$.price").value(40.00));

        mockMvc.perform(get("/listings/" + listingId).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Mini Fridge"))
                .andExpect(jsonPath("$.description").value("Barely used")).andExpect(jsonPath("$.price").value(40.00))
                .andExpect(jsonPath("$.condition").value("LIKE_NEW"))
                .andExpect(jsonPath("$.category").value("KITCHEN"));
    }

    @Test
    void updateListing_notOwner_returnsForbidden() throws Exception {
        Cookie ownerSession = loginAsNewStudent("jane@tulane.edu");
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        MvcResult createResult = mockMvc
                .perform(post("/listings").cookie(ownerSession).contentType("application/json").content(createBody))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Cookie otherSession = loginAsNewStudent("bob@tulane.edu");
        String updateBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

        mockMvc.perform(
                put("/listings/" + listingId).cookie(otherSession).contentType("application/json").content(updateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateListing_listingDoesNotExist_returnsNotFound() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        String updateBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

        mockMvc.perform(put("/listings/999999").cookie(session).contentType("application/json").content(updateBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateListing_rejectsWhenNotLoggedIn() throws Exception {
        String updateBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

        mockMvc.perform(put("/listings/1").contentType("application/json").content(updateBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteListing_owner_removesListing() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        // Keep deletion verification within the owner's authenticated school feed.
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        MvcResult createResult = mockMvc
                .perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/listings/" + listingId).cookie(session)).andExpect(status().isNoContent());

        mockMvc.perform(get("/listings").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteListing_notOwner_returnsForbidden() throws Exception {
        Cookie ownerSession = loginAsNewStudent("jane@tulane.edu");
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        MvcResult createResult = mockMvc
                .perform(post("/listings").cookie(ownerSession).contentType("application/json").content(createBody))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        Cookie otherSession = loginAsNewStudent("bob@tulane.edu");
        mockMvc.perform(delete("/listings/" + listingId).cookie(otherSession)).andExpect(status().isForbidden());
    }

    @Test
    void deleteListing_listingDoesNotExist_returnsNotFound() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");

        mockMvc.perform(delete("/listings/999999").cookie(session)).andExpect(status().isNotFound());
    }

    @Test
    void getCategories_returnsAllCategoryValues() throws Exception {
        mockMvc.perform(get("/listings/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder("FURNITURE", "ELECTRONICS", "KITCHEN",
                        "DECOR", "CLOTHING", "OTHER")));
    }

    @Test
    void listVerifiedSeller_includesSellerVerifiedInFeed() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/listings").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerVerified").value(true));
    }

    @Test
    void listUnverifiedStudent_excludesSellerVerifiedInFeed() throws Exception {
        // jane is a verified requester with a profile who can read the feed.
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student jane = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(jane, "Jane Doe", "CS", null, "tulane.edu"));

        // bob is an unverified seller (no login session, no PIN verification).
        Student bob = new Student("Bob Smith", "bob@tulane.edu", "hashed", null, null);
        studentRepository.save(bob);
        profileRepository.save(new Profile(bob, "Bob Smith", "CS", null, "tulane.edu"));
        Listing bobListing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"), Condition.GOOD,
                Category.FURNITURE, bob);
        listingRepository.save(bobListing);

        mockMvc.perform(get("/listings").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerVerified").value(false));
    }

    @Test
    void getListingById_withVerifiedSeller_returnsSellerVerified() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc
                .perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/listings/" + listingId).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerVerified").value(true));
    }

    @Test
    void getListingById_withUnverifiedStudent_returnsSellerVerifiedFalse() throws Exception {
        // jane is a verified requester with a profile who can read the listing.
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student jane = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(jane, "Jane Doe", "CS", null, "tulane.edu"));

        // bob is an unverified seller (no login session, no PIN verification).
        Student bob = new Student("Bob Smith", "bob@tulane.edu", "hashed", null, null);
        studentRepository.save(bob);
        profileRepository.save(new Profile(bob, "Bob Smith", "CS", null, "tulane.edu"));
        Listing bobListing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"), Condition.GOOD,
                Category.FURNITURE, bob);
        bobListing = listingRepository.save(bobListing);

        mockMvc.perform(get("/listings/" + bobListing.getId()).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerVerified").value(false));
    }

    @Test
    void createListing_setsSellerVerifiedInResponse() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));

        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));

        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sellerVerified").value(true));
    }

    @Test
    void updateListing_preservesSellerVerified() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated());
        Long listingId = objectMapper
                .readTree(mockMvc
                        .perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));
        mockMvc.perform(
                put("/listings/" + listingId).cookie(session).contentType("application/json").content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sellerVerified").value(true));
    }

    @Test
    void getApiListings_setsSellerVerified() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student seller = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        String createBody = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        mockMvc.perform(post("/listings").cookie(session).contentType("application/json").content(createBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/listings").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sellerVerified").value(true));
    }

    @Test
    void response_excludesSensitiveStudentFields() throws Exception {
        // jane is a verified requester with a profile who can read the listing.
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Student jane = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(jane, "Jane Doe", "CS", null, "tulane.edu"));

        // bob is an unverified seller with sensitive account fields populated.
        Student bob = new Student("Bob Smith", "bob@tulane.edu", "hashed-password", null, null);
        bob.setOauthProvider("google");
        bob.setVerificationPin("123456");
        bob.setVerificationPinExpiry(java.time.LocalDateTime.now().plusMinutes(30));
        bob.setTrustedDeviceIds(Set.of("device-1"));
        studentRepository.save(bob);
        profileRepository.save(new Profile(bob, "Bob Smith", "CS", null, "tulane.edu"));
        Listing bobListing = new Listing("Desk Lamp", "Works great", new BigDecimal("10.00"), Condition.GOOD,
                Category.FURNITURE, bob);
        bobListing = listingRepository.save(bobListing);

        MvcResult result = mockMvc.perform(get("/listings/" + bobListing.getId()).cookie(session))
                .andExpect(status().isOk()).andReturn();

        String body = result.getResponse().getContentAsString();

        assertFalse(body.contains("passwordHash"), "passwordHash must not appear in listing JSON");
        assertFalse(body.contains("verificationPin"), "verificationPin must not appear in listing JSON");
        assertFalse(body.contains("verificationPinExpiry"), "verificationPinExpiry must not appear in listing JSON");
        assertFalse(body.contains("trustedDeviceIds"), "trustedDeviceIds must not appear in listing JSON");
        assertFalse(body.contains("oauthProvider"), "oauthProvider must not appear in listing JSON");
        assertFalse(body.contains("email_verified"), "email_verified must not appear in listing JSON");

        assertTrue(body.contains("sellerVerified"), "sellerVerified must be present in listing JSON");
        assertTrue(body.contains("Bob Smith"), "seller name must be present in listing JSON");
    }

    // SSP1-147: listing status

    private Long createListingWithProfile(Cookie session, String email) throws Exception {
        Student seller = studentRepository.findByEmail(email).orElseThrow();
        if (profileRepository.findByStudent_Id(seller.getId()).isEmpty()) {
            profileRepository.save(new Profile(seller, "Jane Doe", "CS", null, "tulane.edu"));
        }
        String body = objectMapper.writeValueAsString(
                new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
        MvcResult result = mockMvc
                .perform(post("/listings").cookie(session).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String statusBody(String value) {
        return value == null ? "{}" : "{\"status\":\"" + value + "\"}";
    }

    private ListingStatus storedStatus(Long listingId) {
        return listingRepository.findById(listingId).orElseThrow().getStatus();
    }

    @Test
    void getListing_includesStatus() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Long listingId = createListingWithProfile(session, "jane@tulane.edu");

        mockMvc.perform(get("/listings/" + listingId).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Listing listing = listingRepository.findById(listingId).orElseThrow();
        listing.setStatus(ListingStatus.PENDING);
        listingRepository.save(listing);
        mockMvc.perform(get("/listings/" + listingId).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        listing = listingRepository.findById(listingId).orElseThrow();
        listing.setStatus(null);
        listingRepository.save(listing);
        mockMvc.perform(get("/listings/" + listingId).cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateStatus_ownerCanMarkPendingSoldAndBackToActive() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Long listingId = createListingWithProfile(session, "jane@tulane.edu");

        for (String value : new String[] { "PENDING", "SOLD", "ACTIVE" }) {
            mockMvc.perform(patch("/listings/" + listingId + "/status").cookie(session).contentType("application/json")
                    .content(statusBody(value))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(value))
                    .andExpect(jsonPath("$.id").value(listingId));
            assertEquals(ListingStatus.valueOf(value), storedStatus(listingId));
        }
    }

    @Test
    void updateStatus_acceptsLowercase() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Long listingId = createListingWithProfile(session, "jane@tulane.edu");

        mockMvc.perform(patch("/listings/" + listingId + "/status").cookie(session).contentType("application/json")
                .content(statusBody("sold"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SOLD"));
        assertEquals(ListingStatus.SOLD, storedStatus(listingId));
    }

    @Test
    void updateStatus_rejectsInternalAndUnknownValues() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");
        Long listingId = createListingWithProfile(session, "jane@tulane.edu");

        for (String value : new String[] { "DELETED", "DRAFT", "INACTIVE", "BANANA", "", "  ", null }) {
            mockMvc.perform(patch("/listings/" + listingId + "/status").cookie(session).contentType("application/json")
                    .content(statusBody(value))).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("status must be one of ACTIVE, PENDING, SOLD"));
            assertEquals(ListingStatus.ACTIVE, storedStatus(listingId));
        }
    }

    @Test
    void updateStatus_nonOwnerIs403() throws Exception {
        Cookie ownerSession = loginAsNewStudent("jane@tulane.edu");
        Long listingId = createListingWithProfile(ownerSession, "jane@tulane.edu");

        Cookie otherSession = loginAsNewStudent("bob@tulane.edu");
        Student bob = studentRepository.findByEmail("bob@tulane.edu").orElseThrow();
        profileRepository.save(new Profile(bob, "Bob", "CS", null, "tulane.edu"));
        mockMvc.perform(patch("/listings/" + listingId + "/status").cookie(otherSession).contentType("application/json")
                .content(statusBody("SOLD"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to modify this listing"));

        bob = studentRepository.findByEmail("bob@tulane.edu").orElseThrow();
        bob.setRole(Role.ADMIN);
        studentRepository.save(bob);
        mockMvc.perform(patch("/listings/" + listingId + "/status").cookie(otherSession).contentType("application/json")
                .content(statusBody("SOLD"))).andExpect(status().isForbidden());

        // A non-owner gets 403 even for an invalid value, so the value check never leaks ahead of ownership.
        mockMvc.perform(patch("/listings/" + listingId + "/status").cookie(otherSession).contentType("application/json")
                .content(statusBody("BANANA"))).andExpect(status().isForbidden());
        assertEquals(ListingStatus.ACTIVE, storedStatus(listingId));
    }

    @Test
    void updateStatus_missingListingIs404() throws Exception {
        Cookie session = loginAsNewStudent("jane@tulane.edu");

        mockMvc.perform(patch("/listings/999999/status").cookie(session).contentType("application/json")
                .content(statusBody("SOLD"))).andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_requiresLogin() throws Exception {
        mockMvc.perform(patch("/listings/1/status").contentType("application/json").content(statusBody("SOLD")))
                .andExpect(status().isUnauthorized());
    }
}
