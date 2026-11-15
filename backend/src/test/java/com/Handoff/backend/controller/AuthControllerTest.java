package com.Handoff.backend.controller;

import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.support.FakeSupabaseAuthClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
  private static final String EMAIL = "jane@tulane.edu";
  private static final String PASSWORD = "Password123!";
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private StudentRepository studentRepository;
  @Autowired
  private ListingRepository listingRepository;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private FakeSupabaseAuthClient supabase;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void cleanDatabase() {
    listingRepository.deleteAll();
    studentRepository.deleteAll();
    supabase.reset();
  }

  private void signup() throws Exception {
    mockMvc.perform(post("/auth/signup").contentType("application/json")
        .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", EMAIL, PASSWORD))))
        .andExpect(status().isCreated());
  }

  private void verifySignup(String deviceId) throws Exception {
    mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
        .content(objectMapper.writeValueAsString(
            new VerificationRequest(EMAIL, FakeSupabaseAuthClient.SIGNUP_OTP, deviceId))))
        .andExpect(status().isOk());
  }

  @Test
  void signup_verify_me_login_logout_fullFlow() throws Exception {
    MvcResult signupResult = mockMvc.perform(post("/auth/signup").contentType("application/json")
        .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", EMAIL, PASSWORD))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.verified").value(false))
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.supabaseUserId").doesNotExist())
        .andReturn();
    assertThat(signupResult.getResponse().getCookie(AuthCookies.ACCESS)).isNull();
    assertThat(studentRepository.findByEmail(EMAIL).orElseThrow().getSupabaseUserId())
        .isEqualTo(supabase.userIdFor(EMAIL));
    // Logging in before the email is confirmed resends the Supabase code instead of
    // signing in.
    mockMvc.perform(post("/auth/login").contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD, "device-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(true))
        .andExpect(jsonPath("$.message")
            .value("Account is unverified. A new verification PIN has been sent to your school email."))
        .andExpect(cookie().doesNotExist(AuthCookies.ACCESS));
    assertThat(supabase.resentSignupOtps()).containsExactly(EMAIL);
    MvcResult verifyResult = mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
        .content(objectMapper.writeValueAsString(
            new VerificationRequest(EMAIL, FakeSupabaseAuthClient.SIGNUP_OTP, "device-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verified").value(true))
        .andExpect(jsonPath("$.emailVerified").value(true))
        .andExpect(cookie().httpOnly(AuthCookies.ACCESS, true))
        .andExpect(cookie().httpOnly(AuthCookies.REFRESH, true))
        .andExpect(cookie().path(AuthCookies.REFRESH, "/auth"))
        .andReturn();
    assertThat(verifyResult.getRequest().getSession(false)).isNull();
    Cookie access = verifyResult.getResponse().getCookie(AuthCookies.ACCESS);
    mockMvc.perform(get("/auth/me").cookie(access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.verified").value(true));
    mockMvc.perform(post("/auth/login").contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD, "device-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false))
        .andExpect(jsonPath("$.studentName").value("Jane Doe"))
        .andExpect(cookie().exists(AuthCookies.ACCESS));
    mockMvc.perform(post("/auth/logout").cookie(access))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge(AuthCookies.ACCESS, 0))
        .andExpect(cookie().maxAge(AuthCookies.REFRESH, 0));
    mockMvc.perform(get("/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void signup_returnsConflict_whenVerifiedEmailAlreadyRegistered() throws Exception {
    signup();
    verifySignup("device-1");
    mockMvc.perform(post("/auth/signup").contentType("application/json")
        .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", EMAIL, PASSWORD))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered"));
  }

  @Test
  void login_fromNewDevice_requiresDevicePin_thenTrustsDevice() throws Exception {
    signup();
    verifySignup("device-1");
    mockMvc.perform(post("/auth/login").contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD, "device-2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(true))
        .andExpect(jsonPath("$.message").value("New device detected. We sent a 6-digit PIN to your school email."))
        .andExpect(cookie().doesNotExist(AuthCookies.ACCESS));
    String devicePin = studentRepository.findByEmail(EMAIL).orElseThrow().getVerificationPin();
    mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
        .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, devicePin, "device-2"))))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthCookies.ACCESS));
    mockMvc.perform(post("/auth/login").contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD, "device-2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false));
  }

  @Test
  void verifyPin_forNewDeviceWithoutLogin_returns401() throws Exception {
    signup();
    verifySignup("device-1");
    mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
        .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, "654321", "device-9"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Your sign-in expired. Please log in again."));
  }

  @Test
  void login_withWrongPassword_returns401() throws Exception {
    signup();
    verifySignup("device-1");
    mockMvc.perform(post("/auth/login").contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "WrongPassword1", "device-1"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void login_migratesAccountCreatedBeforeSupabase() throws Exception {
    Student legacy = new Student("Legacy Lee", EMAIL, passwordEncoder.encode(PASSWORD), null, null);
    legacy.setVerified(true);
    legacy.setEmailVerified(true);
    legacy.trustDevice("device-1");
    studentRepository.save(legacy);
    mockMvc.perform(post("/auth/login").contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD, "device-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false))
        .andExpect(cookie().exists(AuthCookies.ACCESS));
    Student migrated = studentRepository.findByEmail(EMAIL).orElseThrow();
    assertThat(migrated.getSupabaseUserId()).isEqualTo(supabase.userIdFor(EMAIL));
    assertThat(migrated.getPasswordHash()).isNull();
  }

  @Test
  void login_ignoresStaleAccessCookie() throws Exception {
    signup();
    verifySignup("device-1");
    mockMvc.perform(post("/auth/login").cookie(new Cookie(AuthCookies.ACCESS, "expired-token"))
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD, "device-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requiresPin").value(false));
  }

  @Test
  void refresh_withRefreshCookie_setsNewAccessCookie() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc.perform(post("/auth/refresh")
        .cookie(new Cookie(AuthCookies.REFRESH, FakeSupabaseAuthClient.refreshTokenFor(userId))))
        .andExpect(status().isNoContent())
        .andExpect(cookie().value(AuthCookies.ACCESS, FakeSupabaseAuthClient.accessTokenFor(userId)));
  }

  @Test
  void refresh_withoutCookie_returns401AndClearsCookies() throws Exception {
    mockMvc.perform(post("/auth/refresh"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Please log in again."))
        .andExpect(cookie().maxAge(AuthCookies.ACCESS, 0));
  }

  @Test
  void me_withTokenForUnknownStudent_returns401() throws Exception {
    mockMvc.perform(get("/auth/me")
        .cookie(new Cookie(AuthCookies.ACCESS, FakeSupabaseAuthClient.accessTokenFor(UUID.randomUUID()))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void me_withInvalidToken_returns401() throws Exception {
    mockMvc.perform(get("/auth/me").cookie(new Cookie(AuthCookies.ACCESS, "not-a-real-token")))
        .andExpect(status().isUnauthorized());
  }
}
