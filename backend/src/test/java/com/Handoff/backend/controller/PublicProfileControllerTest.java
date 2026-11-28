package com.Handoff.backend.controller;

import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.support.FakeSupabaseAuthClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private StudentRepository studentRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        profileRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void returnsTheReadOnlyProfileOfAStudentInTheSameSchool() throws Exception {
        Student requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu").getStudent();
        Profile target = profile("Jane Doe", "jane@tulane.edu", "tulane.edu");

        mockMvc.perform(get("/api/profiles/students/{studentId}", target.getStudentId()).cookie(session(requester)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(target.getId().intValue()))
                .andExpect(jsonPath("$.studentId").value(target.getStudentId().intValue()))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.major").value("Computer Science")).andExpect(jsonPath("$.bio").value("Bio"))
                .andExpect(jsonPath("$.schoolDomain").value("tulane.edu")).andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.verified").value(true)).andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void hidesStudentsFromAnotherSchool() throws Exception {
        Student requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu").getStudent();
        Profile target = profile("Lee Loyola", "lee@loyola.edu", "loyola.edu");

        mockMvc.perform(get("/api/profiles/students/{studentId}", target.getStudentId()).cookie(session(requester)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Profile not found"));
    }

    @Test
    void returnsNotFoundForAnUnknownStudent() throws Exception {
        Student requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu").getStudent();

        mockMvc.perform(get("/api/profiles/students/{studentId}", 999999L).cookie(session(requester)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Profile not found"));
    }

    @Test
    void returnsNotFoundWhenTheRequesterHasNoProfile() throws Exception {
        Student requester = student("Profileless", "none@tulane.edu");
        Profile target = profile("Jane Doe", "jane@tulane.edu", "tulane.edu");

        mockMvc.perform(get("/api/profiles/students/{studentId}", target.getStudentId()).cookie(session(requester)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Profile not found"));
    }

    @Test
    void requiresAnAuthenticatedSession() throws Exception {
        Profile target = profile("Jane Doe", "jane@tulane.edu", "tulane.edu");

        mockMvc.perform(get("/api/profiles/students/{studentId}", target.getStudentId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User must be authenticated to view profiles."));
    }

    private Cookie session(Student student) {
        if (student.getSupabaseUserId() == null) {
            student.setSupabaseUserId(UUID.randomUUID());
            studentRepository.save(student);
        }
        return new Cookie(AuthCookies.ACCESS, FakeSupabaseAuthClient.accessTokenFor(student.getSupabaseUserId()));
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
