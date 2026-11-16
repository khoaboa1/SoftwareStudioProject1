package com.Handoff.backend.controller;

import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The legacy /students routes have no auth, so they must never change role or suspension. */
@SpringBootTest
@AutoConfigureMockMvc
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void cleanDatabase() {
        listingRepository.deleteAll();
        profileRepository.deleteAll();
        studentRepository.deleteAll();
    }

    private Student saved(String email, Role role, boolean suspended) {
        Student student = new Student("Jane Doe", email, "hashed", null, null);
        student.setRole(role);
        student.setSuspended(suspended);
        return studentRepository.save(student);
    }

    private String body(String email) {
        return "{\"studentName\":\"Jane Doe\",\"email\":\"" + email + "\",\"role\":\"ADMIN\",\"suspended\":false}";
    }

    @Test
    void post_cannotCreateAdminOrSetSuspension() throws Exception {
        mockMvc.perform(post("/students").contentType("application/json").content(
                "{\"studentName\":\"Eve\",\"email\":\"eve@tulane.edu\",\"role\":\"ADMIN\",\"suspended\":true}"))
                .andExpect(status().isCreated());
        Student eve = studentRepository.findByEmail("eve@tulane.edu").orElseThrow();
        assertThat(eve.getRole()).isEqualTo(Role.STUDENT);
        assertThat(eve.isSuspended()).isFalse();
    }

    @Test
    void put_cannotGrantAdmin() throws Exception {
        Student student = saved("jane@tulane.edu", Role.STUDENT, false);
        mockMvc.perform(
                put("/students/" + student.getId()).contentType("application/json").content(body("jane@tulane.edu")))
                .andExpect(status().isOk());
        assertThat(studentRepository.findById(student.getId()).orElseThrow().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void put_cannotUnsuspend() throws Exception {
        Student student = saved("jane@tulane.edu", Role.STUDENT, true);
        mockMvc.perform(
                put("/students/" + student.getId()).contentType("application/json").content(body("jane@tulane.edu")))
                .andExpect(status().isOk());
        assertThat(studentRepository.findById(student.getId()).orElseThrow().isSuspended()).isTrue();
    }

    @Test
    void put_doesNotDemoteAdmin() throws Exception {
        Student admin = saved("admin@tulane.edu", Role.ADMIN, false);
        mockMvc.perform(put("/students/" + admin.getId()).contentType("application/json")
                .content("{\"studentName\":\"Khoa Le\",\"email\":\"admin@tulane.edu\"}")).andExpect(status().isOk());
        assertThat(studentRepository.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
}
