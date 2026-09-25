package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateListingRequest;
import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Student;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    listingRepository.deleteAll();
    studentRepository.deleteAll();
  }

  private MockHttpSession loginAsNewStudent(String email) throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", email, "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    Student student = studentRepository.findByEmail(email).orElseThrow();
    VerificationRequest verifyBody = new VerificationRequest(email, student.getVerificationPin(), "test-device");
    MvcResult verifyResult = mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isOk())
        .andReturn();

    HttpSession session = verifyResult.getRequest().getSession(false);
    return (MockHttpSession) session;
  }

  @Test
  void createListing_thenFeed_returnsItNewestFirst() throws Exception {
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");

    String body = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));

    mockMvc.perform(post("/listings").session(session).contentType("application/json").content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.itemName").value("Desk Lamp"))
        .andExpect(jsonPath("$.sellerName").value("Jane Doe"));

    mockMvc.perform(get("/listings"))
        .andExpect(status().isOk())
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
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");

    String body = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "SHINY", "FURNITURE"));

    mockMvc.perform(post("/listings").session(session).contentType("application/json").content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createListing_rejectsNonPositivePrice() throws Exception {
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");

    String body = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("0"), "GOOD", "FURNITURE"));

    mockMvc.perform(post("/listings").session(session).contentType("application/json").content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateListing_owner_updatesFields() throws Exception {
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");
    String createBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
    MvcResult createResult = mockMvc.perform(post("/listings").session(session)
            .contentType("application/json").content(createBody))
        .andExpect(status().isCreated())
        .andReturn();
    Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

    String updateBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

    mockMvc.perform(put("/listings/" + listingId).session(session)
            .contentType("application/json").content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemName").value("Mini Fridge"))
        .andExpect(jsonPath("$.price").value(40.00));
  }

  @Test
  void updateListing_notOwner_returnsForbidden() throws Exception {
    MockHttpSession ownerSession = loginAsNewStudent("jane@tulane.edu");
    String createBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
    MvcResult createResult = mockMvc.perform(post("/listings").session(ownerSession)
            .contentType("application/json").content(createBody))
        .andExpect(status().isCreated())
        .andReturn();
    Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

    MockHttpSession otherSession = loginAsNewStudent("bob@tulane.edu");
    String updateBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

    mockMvc.perform(put("/listings/" + listingId).session(otherSession)
            .contentType("application/json").content(updateBody))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateListing_listingDoesNotExist_returnsNotFound() throws Exception {
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");
    String updateBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Mini Fridge", "Barely used", new BigDecimal("40.00"), "LIKE_NEW", "KITCHEN"));

    mockMvc.perform(put("/listings/999999").session(session)
            .contentType("application/json").content(updateBody))
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
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");
    String createBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
    MvcResult createResult = mockMvc.perform(post("/listings").session(session)
            .contentType("application/json").content(createBody))
        .andExpect(status().isCreated())
        .andReturn();
    Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/listings/" + listingId).session(session))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/listings"))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void deleteListing_notOwner_returnsForbidden() throws Exception {
    MockHttpSession ownerSession = loginAsNewStudent("jane@tulane.edu");
    String createBody = objectMapper.writeValueAsString(
        new CreateListingRequest("Desk Lamp", "Works great", new BigDecimal("10.00"), "GOOD", "FURNITURE"));
    MvcResult createResult = mockMvc.perform(post("/listings").session(ownerSession)
            .contentType("application/json").content(createBody))
        .andExpect(status().isCreated())
        .andReturn();
    Long listingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

    MockHttpSession otherSession = loginAsNewStudent("bob@tulane.edu");
    mockMvc.perform(delete("/listings/" + listingId).session(otherSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteListing_listingDoesNotExist_returnsNotFound() throws Exception {
    MockHttpSession session = loginAsNewStudent("jane@tulane.edu");

    mockMvc.perform(delete("/listings/999999").session(session))
        .andExpect(status().isNotFound());
  }

  @Test
  void getCategories_returnsAllCategoryValues() throws Exception {
    mockMvc.perform(get("/listings/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder(
            "FURNITURE", "ELECTRONICS", "KITCHEN", "DECOR", "CLOTHING", "OTHER")));
  }
}
