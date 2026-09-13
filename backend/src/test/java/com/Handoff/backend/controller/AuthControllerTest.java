package com.Handoff.backend.controller;

import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.repository.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private StudentRepository studentRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    studentRepository.deleteAll();
  }

  @Test
  void signup_thenLogin_thenMe_thenLogout_fullFlow() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));

    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    String loginBody = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!"));

    MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentName").value("Jane Doe"))
        .andReturn();

    HttpSession session = loginResult.getRequest().getSession(false);

    mockMvc.perform(get("/auth/me").session((MockHttpSession) session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"));

    mockMvc.perform(post("/auth/logout").session((MockHttpSession) session))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/auth/me").session((MockHttpSession) session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void signup_returnsConflict_whenEmailAlreadyRegistered() throws Exception {
    String body = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));

    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered"));
  }

  @Test
  void signup_rejectsNonEduEmail() throws Exception {
    String body = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@gmail.com", "Password123!"));

    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void login_returnsUnauthorized_whenPasswordWrong() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    String loginBody = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "WrongPassword1"));

    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void login_returnsUnauthorized_whenEmailUnknown() throws Exception {
    String loginBody = objectMapper.writeValueAsString(
        new LoginRequest("nobody@tulane.edu", "Password123!"));

    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void login_returnsUnauthorized_forOauthOnlyUser() throws Exception {
    // Create an OAuth-only student (no password)
    com.Handoff.backend.model.Student oauthStudent =
        new com.Handoff.backend.model.Student("OAuth User", "oauth@tulane.edu", null, "google", null, null);
    studentRepository.save(oauthStudent);

    String loginBody = objectMapper.writeValueAsString(
        new LoginRequest("oauth@tulane.edu", "SomePassword1"));

    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("This account has no password set."));
  }

  @Test
  void verifyPin_succeedsAndEstablishesSession() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    com.Handoff.backend.model.Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    String pin = saved.getVerificationPin();

    com.Handoff.backend.dto.VerificationRequest verifyBody =
        new com.Handoff.backend.dto.VerificationRequest("jane@tulane.edu", pin, "device-browser-1");

    MvcResult result = mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andReturn();

    HttpSession session = result.getRequest().getSession(false);
    mockMvc.perform(get("/auth/me").session((MockHttpSession) session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"));
  }

  @Test
  void verifyPin_failsWithIncorrectPin() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    com.Handoff.backend.dto.VerificationRequest verifyBody =
        new com.Handoff.backend.dto.VerificationRequest("jane@tulane.edu", "000000", "device-browser-1");

    mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Incorrect verification PIN"));
  }

  @Test
  void login_withNewDevice_triggersPinChallenge() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    // Verify first device
    com.Handoff.backend.model.Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(
            new com.Handoff.backend.dto.VerificationRequest("jane@tulane.edu", saved.getVerificationPin(), "device-1"))))
        .andExpect(status().isOk());

    // Login with same device-1 -> succeeds directly without pin challenge
    String loginBodySameDevice = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!", "device-1"));
    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBodySameDevice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false))
        .andExpect(jsonPath("$.studentName").value("Jane Doe"));

    // Login with new device-2 -> triggers pin challenge
    String loginBodyNewDevice = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!", "device-2"));
    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBodyNewDevice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(true));
  }
}
