package com.Handoff.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.support.FakeSupabaseAuthClient;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

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

    private String currentPin() {
        return studentRepository.findByEmail(EMAIL).orElseThrow().getVerificationPin();
    }

    /** Verifies the sign-up PIN. Our PIN is read from the database, the same value printed in the console. */
    private void verifySignup(String deviceId) throws Exception {
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, currentPin(), deviceId))))
                .andExpect(status().isOk());
    }

    private LoginRequest login(String password, String deviceId) {
        return new LoginRequest(EMAIL, password, deviceId);
    }

    @Test
    void signup_verify_login_me_logout_fullFlow() throws Exception {
        MvcResult signupResult = mockMvc
                .perform(post("/auth/signup").contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", EMAIL, PASSWORD))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.verified").value(false)).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.supabaseUserId").doesNotExist())
                .andExpect(jsonPath("$.verificationPin").doesNotExist()).andReturn();
        assertThat(signupResult.getResponse().getCookie(AuthCookies.ACCESS)).isNull();
        Student created = studentRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(created.getSupabaseUserId()).isEqualTo(supabase.userIdFor(EMAIL));
        assertThat(created.getVerificationPin()).matches("\\d{6}");
        assertThat(supabase.isConfirmed(EMAIL)).isFalse();
        // Logging in before the PIN is entered sends a new PIN instead of signing in.
        mockMvc.perform(post("/auth/login").contentType("application/json")
                .content(objectMapper.writeValueAsString(login(PASSWORD, "device-1")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresPin").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Account is unverified. A new verification PIN has been sent to your school email."))
                .andExpect(cookie().doesNotExist(AuthCookies.ACCESS));
        // Verifying our PIN confirms the account but issues no cookies: the client signs in next.
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, currentPin(), "device-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.emailVerified").value(true)).andExpect(cookie().doesNotExist(AuthCookies.ACCESS))
                .andExpect(cookie().doesNotExist(AuthCookies.REFRESH));
        assertThat(supabase.isConfirmed(EMAIL)).isTrue();
        MvcResult loginResult = mockMvc
                .perform(post("/auth/login").contentType("application/json")
                        .content(objectMapper.writeValueAsString(login(PASSWORD, "device-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiresPin").value(false))
                .andExpect(jsonPath("$.studentName").value("Jane Doe"))
                .andExpect(cookie().httpOnly(AuthCookies.ACCESS, true))
                .andExpect(cookie().httpOnly(AuthCookies.REFRESH, true))
                .andExpect(cookie().path(AuthCookies.REFRESH, "/auth")).andReturn();
        assertThat(loginResult.getRequest().getSession(false)).isNull();
        Cookie access = loginResult.getResponse().getCookie(AuthCookies.ACCESS);
        mockMvc.perform(get("/auth/me").cookie(access)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL)).andExpect(jsonPath("$.verified").value(true));
        mockMvc.perform(post("/auth/logout").cookie(access)).andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthCookies.ACCESS, 0)).andExpect(cookie().maxAge(AuthCookies.REFRESH, 0));
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void signup_returnsConflict_whenVerifiedEmailAlreadyRegistered() throws Exception {
        signup();
        verifySignup("device-1");
        mockMvc.perform(post("/auth/signup").contentType("application/json")
                .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", EMAIL, PASSWORD))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void signup_again_beforeVerifying_reusesTheSupabaseUserAndSendsANewPin() throws Exception {
        signup();
        UUID firstUserId = supabase.userIdFor(EMAIL);
        signup();
        assertThat(supabase.userIdFor(EMAIL)).isEqualTo(firstUserId);
        assertThat(studentRepository.findByEmail(EMAIL).orElseThrow().getVerificationPin()).matches("\\d{6}");
    }

    @Test
    void verifyPin_withWrongPin_returns400AndKeepsAccountUnconfirmed() throws Exception {
        signup();
        String wrongPin = "000000".equals(currentPin()) ? "111111" : "000000";
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, wrongPin, "device-1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect verification PIN"));
        assertThat(supabase.isConfirmed(EMAIL)).isFalse();
    }

    @Test
    void resendPin_forUnverifiedAccount_returnsNoContent() throws Exception {
        signup();
        mockMvc.perform(
                post("/auth/resend-pin").contentType("application/json").content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(currentPin()).matches("\\d{6}");
    }

    @Test
    void login_fromNewDevice_requiresDevicePin_thenTrustsDevice() throws Exception {
        signup();
        verifySignup("device-1");
        mockMvc.perform(post("/auth/login").contentType("application/json")
                .content(objectMapper.writeValueAsString(login(PASSWORD, "device-2")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresPin").value(true))
                .andExpect(
                        jsonPath("$.message").value("New device detected. We sent a 6-digit PIN to your school email."))
                .andExpect(cookie().doesNotExist(AuthCookies.ACCESS));
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, currentPin(), "device-2"))))
                .andExpect(status().isOk()).andExpect(cookie().exists(AuthCookies.ACCESS));
        mockMvc.perform(post("/auth/login").contentType("application/json")
                .content(objectMapper.writeValueAsString(login(PASSWORD, "device-2")))).andExpect(status().isOk())
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
                .content(objectMapper.writeValueAsString(login("WrongPassword1", "device-1"))))
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
                .content(objectMapper.writeValueAsString(login(PASSWORD, "device-1")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresPin").value(false)).andExpect(cookie().exists(AuthCookies.ACCESS));
        Student migrated = studentRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(migrated.getSupabaseUserId()).isEqualTo(supabase.userIdFor(EMAIL));
        assertThat(migrated.getPasswordHash()).isNull();
    }

    @Test
    void login_ignoresStaleAccessCookie() throws Exception {
        signup();
        verifySignup("device-1");
        mockMvc.perform(post("/auth/login").cookie(new Cookie(AuthCookies.ACCESS, "expired-token"))
                .contentType("application/json").content(objectMapper.writeValueAsString(login(PASSWORD, "device-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiresPin").value(false));
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
        mockMvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized())
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

    @Test
    void role_isStudentInSignupVerifyLoginAndMe() throws Exception {
        mockMvc.perform(post("/auth/signup").contentType("application/json")
                .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", EMAIL, PASSWORD))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT"));
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(new VerificationRequest(EMAIL, currentPin(), "device-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"));
        MvcResult loginResult = mockMvc
                .perform(post("/auth/login").contentType("application/json")
                        .content(objectMapper.writeValueAsString(login(PASSWORD, "device-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.requiresPin").value(false))
                .andExpect(jsonPath("$.role").value("STUDENT")).andReturn();
        Cookie access = loginResult.getResponse().getCookie(AuthCookies.ACCESS);
        mockMvc.perform(get("/auth/me").cookie(access)).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void role_isAdminInLoginAndMe_whenStoredRoleIsAdmin() throws Exception {
        signup();
        verifySignup("device-1");
        Student student = studentRepository.findByEmail(EMAIL).orElseThrow();
        student.setRole(Role.ADMIN);
        studentRepository.save(student);
        MvcResult loginResult = mockMvc
                .perform(post("/auth/login").contentType("application/json")
                        .content(objectMapper.writeValueAsString(login(PASSWORD, "device-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN")).andReturn();
        Cookie access = loginResult.getResponse().getCookie(AuthCookies.ACCESS);
        mockMvc.perform(get("/auth/me").cookie(access)).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_requiringPin_hasNoRole() throws Exception {
        signup();
        mockMvc.perform(post("/auth/login").contentType("application/json")
                .content(objectMapper.writeValueAsString(login(PASSWORD, "device-1")))).andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresPin").value(true)).andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(cookie().doesNotExist(AuthCookies.ACCESS));
    }
}
