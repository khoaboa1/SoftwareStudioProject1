package com.Handoff.backend.controller;

import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
  private ListingRepository listingRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    listingRepository.deleteAll();
    studentRepository.deleteAll();
  }

  @Test
  void signup_thenLogin_thenMe_thenLogout_fullFlow() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));

    // 1. Signup creates unverified account
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.verified").value(false))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    // 2. Unverified login requires PIN
    String loginBody = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!"));

    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(true));

    // 3. Verify PIN
    Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    String pin = saved.getVerificationPin();

    VerificationRequest verifyBody = new VerificationRequest("jane@tulane.edu", pin, "device-1");
    MvcResult verifyResult = mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verified").value(true))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andReturn();

    // 4. Subsequent login succeeds without requiring PIN
    MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false))
        .andExpect(jsonPath("$.verified").value(true))
        .andExpect(jsonPath("$.studentName").value("Jane Doe"))
        .andReturn();

    HttpSession session = loginResult.getRequest().getSession(false);

    mockMvc.perform(get("/auth/me").session((MockHttpSession) session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.verified").value(true));

    mockMvc.perform(post("/auth/logout").session((MockHttpSession) session))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/auth/me").session((MockHttpSession) session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void signup_returnsConflict_whenVerifiedEmailAlreadyRegistered() throws Exception {
    String body = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));

    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isCreated());

    // Mark verified
    Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    saved.setVerified(true);
    studentRepository.save(saved);

    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered"));
  }

  @Test
  void signup_whenUnverified_refreshesPinAndAccount() throws Exception {
    String body1 = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body1))
        .andExpect(status().isCreated());

    Student firstSaved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    String firstPin = firstSaved.getVerificationPin();

    String body2 = objectMapper.writeValueAsString(
        new SignupRequest("Jane Updated", "jane@tulane.edu", "NewPassword123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(body2))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.studentName").value("Jane Updated"));

    Student secondSaved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    assertThat(secondSaved.getStudentName()).isEqualTo("Jane Updated");
    // Old PIN was replaced by fresh PIN
    assertThat(secondSaved.getVerificationPin()).isNotNull();
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
    Student oauthStudent =
        new Student("OAuth User", "oauth@tulane.edu", null, "google", null, null);
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

    Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    String pin = saved.getVerificationPin();

    VerificationRequest verifyBody =
        new VerificationRequest("jane@tulane.edu", pin, "device-browser-1");

    MvcResult result = mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verified").value(true))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andReturn();

    HttpSession session = result.getRequest().getSession(false);
    mockMvc.perform(get("/auth/me").session((MockHttpSession) session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.verified").value(true));
  }

  @Test
  void verifyPin_failsWithIncorrectPin() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    VerificationRequest verifyBody =
        new VerificationRequest("jane@tulane.edu", "000000", "device-browser-1");

    mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Incorrect verification PIN"));
  }

  @Test
  void login_verifiedUser_neverRequiresPinEvenOnNewDevice() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    // Verify account
    Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(
            new VerificationRequest("jane@tulane.edu", saved.getVerificationPin(), "device-1"))))
        .andExpect(status().isOk());

    // Login with device-1 -> succeeds directly without pin
    String loginDevice1 = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!", "device-1"));
    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginDevice1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false))
        .andExpect(jsonPath("$.verified").value(true));

    // Login with new device-2 -> once verified, user is NOT asked for PIN next time they sign in!
    String loginDevice2 = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!", "device-2"));
    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginDevice2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false))
        .andExpect(jsonPath("$.verified").value(true));
  }

  @Test
  void login_unverifiedUserExpiredAfter30Minutes_deletesAccountAndReturnsError() throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", "jane@tulane.edu", "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());

    // Age the unverified account past 30 minutes
    Student saved = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();
    saved.setCreatedAt(LocalDateTime.now().minusMinutes(35));
    studentRepository.save(saved);

    String loginBody = objectMapper.writeValueAsString(
        new LoginRequest("jane@tulane.edu", "Password123!"));

    mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Verification expired after 30 minutes. Please sign up again."));

    // Verify account was deleted from database
    assertThat(studentRepository.findByEmail("jane@tulane.edu")).isEmpty();
  }
}
