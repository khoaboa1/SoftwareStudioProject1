package com.Handoff.backend.controller;

import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileSearchControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private StudentRepository studentRepository;

  @BeforeEach
  void cleanDatabase() {
    profileRepository.deleteAll();
    studentRepository.deleteAll();
  }

  @Test
  void returnsCompactMatchingProfilesAndMetadata() throws Exception {
    Student requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu").getStudent();
    profile("Jane Doe", "jane@tulane.edu", "tulane.edu");

    mockMvc.perform(get("/api/profiles/search")
            .session(session(requester))
            .param("q", "jane"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("Jane Doe"))
        .andExpect(jsonPath("$.items[0].studentId").isNumber())
        .andExpect(jsonPath("$.items[0].schoolDomain").doesNotExist())
        .andExpect(jsonPath("$.meta.currentPage").value(1))
        .andExpect(jsonPath("$.meta.pageSize").value(20));
  }

  @Test
  void returnsAnEmptyPageInsteadOfNotFound() throws Exception {
    Student requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu").getStudent();

    mockMvc.perform(get("/api/profiles/search")
            .session(session(requester))
            .param("q", "nobody"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.meta.totalItems").value(0));
  }

  @Test
  void rejectsBlankQueriesWithTheStandardErrorShape() throws Exception {
    Student requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu").getStudent();

    mockMvc.perform(get("/api/profiles/search")
            .session(session(requester))
            .param("q", "   "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("q is required"));
  }

  @Test
  void requiresAnAuthenticatedSession() throws Exception {
    mockMvc.perform(get("/api/profiles/search").param("q", "jane"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("User must be authenticated to search profiles."));
  }

  @Test
  void returnsNotFoundWhenTheRequesterHasNoProfile() throws Exception {
    Student requester = student("Profileless", "none@tulane.edu");

    mockMvc.perform(get("/api/profiles/search")
            .session(session(requester))
            .param("q", "jane"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Profile not found"));
  }

  private MockHttpSession session(Student student) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
    return session;
  }

  private Profile profile(String name, String email, String domain) {
    Student student = student(name, email);
    return profileRepository.save(new Profile(student, name, "Computer Science", "Bio", domain));
  }

  private Student student(String name, String email) {
    Student student = new Student(name, email, "hashed", null, null);
    student.setVerified(true);
    return studentRepository.save(student);
  }
}
