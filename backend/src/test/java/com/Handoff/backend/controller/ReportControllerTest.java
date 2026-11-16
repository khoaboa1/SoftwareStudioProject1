package com.Handoff.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ConversationRepository;
import com.Handoff.backend.repository.ListingReportRepository;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {
  @Autowired MockMvc mvc;
  @Autowired StudentRepository students;
  @Autowired ProfileRepository profiles;
  @Autowired ListingRepository listings;
  @Autowired ListingReportRepository reports;
  @Autowired ConversationRepository conversations;

  @BeforeEach void clean() {
    reports.deleteAll();
    conversations.deleteAll();
    listings.deleteAll();
    profiles.deleteAll();
    students.deleteAll();
  }
  @AfterEach void cleanAfter() { clean(); }
  private Student student(String name, String email, String domain) {
    Student student = students.save(new Student(name, email, "hash", null, null));
    profiles.save(new Profile(student, name, "CS", null, domain));
    return student;
  }
  private MockHttpSession session(Student student) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(SessionKeys.STUDENT_ID, student.getId());
    return session;
  }
  private Listing listing(Student seller) {
    return listings.save(new Listing("Lamp", "Works", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller));
  }

  @Test void reportCreatesOpenThenDuplicateConflictsAndDeleteCascades() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Student reporter = student("Reporter", "reporter@tulane.edu", "tulane.edu");
    Listing item = listing(seller);
    String url = "/listings/" + item.getId() + "/reports";
    mvc.perform(post(url).contentType("application/json").content("{\"reason\":\"SCAM\",\"details\":\" suspicious \"}"))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("You must be logged in to report a listing"));
    mvc.perform(post(url).session(session(reporter)).contentType("application/json")
        .content("{\"reason\":\"SCAM\",\"details\":\" suspicious \"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.details").value("suspicious"));
    mvc.perform(post(url).session(session(reporter)).contentType("application/json")
        .content("{\"reason\":\"SCAM\"}"))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("You have already reported this listing"));
    mvc.perform(delete("/listings/" + item.getId()).session(session(seller)))
        .andExpect(status().isNoContent());
    assertThat(reports.count()).isZero();
  }

  @Test void reportRejectsOwnInvalidAndOtherSchool() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Student other = student("Other", "other@usf.edu", "usf.edu");
    Listing item = listing(seller);
    String url = "/listings/" + item.getId() + "/reports";
    mvc.perform(post(url).session(session(seller)).contentType("application/json")
        .content("{\"reason\":\"SPAM\"}"))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("You cannot report your own listing"));
    mvc.perform(post(url).session(session(other)).contentType("application/json")
        .content("{\"reason\":\"SPAM\"}"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Listing not found"));
    mvc.perform(post(url).session(session(seller)).contentType("application/json")
        .content("{\"reason\":\"bad\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("reason must be one of SCAM, PROHIBITED_ITEM, SPAM, OFFENSIVE, OTHER"));
  }
}
