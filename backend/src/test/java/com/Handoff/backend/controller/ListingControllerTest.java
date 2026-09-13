package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateListingRequest;
import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.StudentRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    String loginBody = objectMapper.writeValueAsString(new LoginRequest(email, "Password123!"));
    MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isOk())
        .andReturn();

    HttpSession session = loginResult.getRequest().getSession(false);
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
}
