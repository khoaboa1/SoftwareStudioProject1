package com.Handoff.backend.controller;

import com.Handoff.backend.dto.CreateProfileRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private StudentRepository studentRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private ListingRepository listingRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  @AfterEach
  void cleanDatabase() {
    listingRepository.deleteAll();
    profileRepository.deleteAll();
    studentRepository.deleteAll();
  }

  /**
   * Helper method to simulate a verified user registration and login session.
   */
  private MockHttpSession loginAsNewStudent(String name, String email) throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest(name, email, "Password123!"));
    mockMvc.perform(post("/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(signupBody))
        .andExpect(status().isCreated());

    Student student = studentRepository.findByEmail(email).orElseThrow();
    VerificationRequest verifyBody = new VerificationRequest(email, student.getVerificationPin(), "test-device");
    MvcResult verifyResult = mockMvc.perform(post("/auth/verify-pin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isOk())
        .andReturn();

    return (MockHttpSession) verifyResult.getRequest().getSession(false);
  }

  // =========================================================================
  // SUCCESS TEST CASES
  // =========================================================================

  @Test
  @DisplayName("Success - Profile Creation with full valid payload returns 201 and created profile")
  void testCreateProfile_Success() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    CreateProfileRequest request = new CreateProfileRequest(
        "Jane Doe",
        "Computer Science",
        "Junior studying CS and Math."
    );

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Jane Doe"))
        .andExpect(jsonPath("$.major").value("Computer Science"))
        .andExpect(jsonPath("$.bio").value("Junior studying CS and Math."))
        .andExpect(jsonPath("$.schoolDomain").value("tulane.edu"))
        .andExpect(jsonPath("$.studentId").isNumber());

    assertThat(profileRepository.count()).isEqualTo(1);
    Profile saved = profileRepository.findAll().get(0);
    assertThat(saved.getName()).isEqualTo("Jane Doe");
    assertThat(saved.getMajor()).isEqualTo("Computer Science");
    assertThat(saved.getSchoolDomain()).isEqualTo("tulane.edu");
  }

  @Test
  @DisplayName("Success - Domain Extraction accurately parses school domain from authenticated email")
  void testCreateProfile_DomainExtraction() throws Exception {
    MockHttpSession session = loginAsNewStudent("Bob Miller", "bob@law.tulane.edu");

    CreateProfileRequest request = new CreateProfileRequest("Bob Miller", "Law", null);

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.schoolDomain").value("law.tulane.edu"));

    Profile saved = profileRepository.findAll().get(0);
    assertThat(saved.getSchoolDomain()).isEqualTo("law.tulane.edu");
  }

  @Test
  @DisplayName("Success - Profile Creation with optional bio omitted or null")
  void testCreateProfile_OptionalBio() throws Exception {
    MockHttpSession session = loginAsNewStudent("Alex Smith", "alex@tulane.edu");

    CreateProfileRequest request = new CreateProfileRequest("Alex Smith", "Finance", null);

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Alex Smith"))
        .andExpect(jsonPath("$.major").value("Finance"))
        .andExpect(jsonPath("$.bio").isEmpty());
  }

  // =========================================================================
  // ERROR TEST CASES
  // =========================================================================

  @Test
  @DisplayName("Error - Unauthenticated request returns 401 Unauthorized")
  void testCreateProfile_Unauthenticated() throws Exception {
    CreateProfileRequest request = new CreateProfileRequest("Anonymous", "Undeclared", "Bio");

    mockMvc.perform(post("/api/profiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("User must be authenticated to create a profile."));
  }

  @Test
  @DisplayName("Error - Validation Failure when name is missing or blank returns 400 Bad Request")
  void testCreateProfile_MissingName() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    String payload = """
        {
          "name": "",
          "major": "Computer Science",
          "bio": "Some bio"
        }
        """;

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Error - Validation Failure when major is missing or blank returns 400 Bad Request")
  void testCreateProfile_MissingMajor() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    String payload = """
        {
          "name": "Jane Doe",
          "major": "   ",
          "bio": "Some bio"
        }
        """;

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Error - Validation Failure when field exceeds character limit returns 400 Bad Request")
  void testCreateProfile_CharacterLimitExceeded() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    String longBio = "a".repeat(1001);
    CreateProfileRequest request = new CreateProfileRequest("Jane Doe", "CS", longBio);

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Error - Missing request body returns 400 Bad Request")
  void testCreateProfile_MissingBody() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Error - Duplicate Profile returns 409 Conflict")
  void testCreateProfile_DuplicateProfile() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    CreateProfileRequest request = new CreateProfileRequest("Jane Doe", "Computer Science", "Bio");

    // First attempt -> Success
    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Second attempt -> Conflict
    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("A profile already exists for this account."));
  }

  // =========================================================================
  // SECURITY & DATA INTEGRITY TEST CASES
  // =========================================================================

  @Test
  @DisplayName("Security - Data Integrity ignores manually injected user_id and school_domain")
  void testCreateProfile_IgnoresManualInjections() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");
    Student authenticatedStudent = studentRepository.findByEmail("jane@tulane.edu").orElseThrow();

    // Payload maliciously contains user_id 9999 and school_domain 'hacker.edu'
    String maliciousPayload = """
        {
          "name": "Jane Doe",
          "major": "Cybersecurity",
          "bio": "Testing security",
          "user_id": 9999,
          "student_id": 9999,
          "studentId": 9999,
          "school_domain": "hacker.edu",
          "schoolDomain": "hacker.edu"
        }
        """;

    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(maliciousPayload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.schoolDomain").value("tulane.edu"))
        .andExpect(jsonPath("$.studentId").value(authenticatedStudent.getId()));

    Profile saved = profileRepository.findAll().get(0);
    assertThat(saved.getStudent().getId()).isEqualTo(authenticatedStudent.getId());
    assertThat(saved.getSchoolDomain()).isEqualTo("tulane.edu");
    assertThat(saved.getSchoolDomain()).isNotEqualTo("hacker.edu");
  }

  // =========================================================================
  // PROFILE RETRIEVAL TEST CASES (SSP1-68)
  // =========================================================================

  @Test
  @DisplayName("Success - Fetch own profile returns 200 OK with complete profile payload")
  void testGetMyProfile_Success() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");

    CreateProfileRequest request = new CreateProfileRequest(
        "Jane Doe",
        "Computer Science",
        "Junior studying CS and Math."
    );

    // Create profile first
    mockMvc.perform(post("/api/profiles")
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // Now fetch it
    mockMvc.perform(get("/api/profiles/me")
            .session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Jane Doe"))
        .andExpect(jsonPath("$.major").value("Computer Science"))
        .andExpect(jsonPath("$.bio").value("Junior studying CS and Math."))
        .andExpect(jsonPath("$.schoolDomain").value("tulane.edu"))
        .andExpect(jsonPath("$.studentId").isNumber())
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("Error - Not Found when authenticated user has no profile returns 404")
  void testGetMyProfile_NotFound() throws Exception {
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");
    // No profile created

    mockMvc.perform(get("/api/profiles/me")
            .session(session))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Profile not found"));
  }

  @Test
  @DisplayName("Security - Missing token returns 401 Unauthorized")
  void testGetMyProfile_MissingToken() throws Exception {
    // No session/cookie
    mockMvc.perform(get("/api/profiles/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  @DisplayName("Security - Invalid/expired token returns 401 Unauthorized")
  void testGetMyProfile_InvalidToken() throws Exception {
    // Case 1: Bogus JSESSIONID cookie
    mockMvc.perform(get("/api/profiles/me")
            .cookie(new Cookie("JSESSIONID", "tampered-invalid-session")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").exists());

    // Case 2: Expired/invalidated session
    MockHttpSession session = loginAsNewStudent("Jane Doe", "jane@tulane.edu");
    session.invalidate(); // simulate expired session

    mockMvc.perform(get("/api/profiles/me")
            .session(session))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").exists());
  }
}
