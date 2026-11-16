package com.Handoff.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingReport;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.ReportReason;
import com.Handoff.backend.model.Role;
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
class AdminControllerTest {
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

  private Student student(String name, String email, String domain, Role role) {
    Student student = new Student(name, email, "hash", null, null);
    student.setRole(role);
    student = students.save(student);
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

  @Test void adminRoutesRejectAnonymousAndStudent() throws Exception {
    Student normal = student("Normal", "normal@tulane.edu", "tulane.edu", Role.STUDENT);
    String[] paths = {"/api/admin/reports", "/api/admin/reports/1/dismiss", "/api/admin/students",
        "/api/admin/students/1/suspend", "/api/admin/students/1/unsuspend", "/api/admin/students/1/role"};
    for (int i = 0; i < paths.length; i++) {
      var anonymous = i == 0 || i == 2 ? get(paths[i]) : i == 5 ? put(paths[i]).contentType("application/json").content("{\"role\":\"ADMIN\"}") : post(paths[i]);
      mvc.perform(anonymous).andExpect(status().isUnauthorized());
      var logged = i == 0 || i == 2 ? get(paths[i]) : i == 5 ? put(paths[i]).contentType("application/json").content("{\"role\":\"ADMIN\"}") : post(paths[i]);
      mvc.perform(logged.session(session(normal))).andExpect(status().isForbidden())
          .andExpect(jsonPath("$.message").value("Admin access required"));
    }
  }

  @Test void reportsAndStudentActionsAreScopedToSchool() throws Exception {
    Student admin = student("Admin", "admin@tulane.edu", "tulane.edu", Role.ADMIN);
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu", Role.STUDENT);
    Student other = student("Other", "other@usf.edu", "usf.edu", Role.STUDENT);
    ListingReport local = reports.save(new ListingReport(listing(seller), admin, ReportReason.SCAM, null));
    ListingReport remote = reports.save(new ListingReport(listing(other), admin, ReportReason.SPAM, null));
    mvc.perform(get("/api/admin/reports").session(session(admin))).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(local.getId()))
        .andExpect(jsonPath("$[1]").doesNotExist());
    mvc.perform(post("/api/admin/reports/" + remote.getId() + "/dismiss").session(session(admin)))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Report not found"));
    mvc.perform(post("/api/admin/reports/" + local.getId() + "/dismiss").session(session(admin)))
        .andExpect(status().isNoContent());
    mvc.perform(post("/api/admin/students/" + other.getId() + "/suspend").session(session(admin)))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Student not found"));
    mvc.perform(get("/api/admin/students").session(session(admin))).andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.totalItems").value(2));
  }

  @Test void roleAndSuspensionTakeEffectOnNextRequest() throws Exception {
    Student admin = student("Admin", "admin@tulane.edu", "tulane.edu", Role.ADMIN);
    Student target = student("Target", "target@tulane.edu", "tulane.edu", Role.STUDENT);
    Listing item = listing(target);
    mvc.perform(put("/api/admin/students/" + target.getId() + "/role").session(session(admin))
        .contentType("application/json").content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
    mvc.perform(get("/api/admin/reports").session(session(target))).andExpect(status().isOk());
    mvc.perform(post("/api/admin/students/" + target.getId() + "/suspend").session(session(admin)))
        .andExpect(status().isConflict());
    mvc.perform(put("/api/admin/students/" + target.getId() + "/role").session(session(admin))
        .contentType("application/json").content("{\"role\":\"STUDENT\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/admin/reports").session(session(target))).andExpect(status().isForbidden());
    mvc.perform(post("/api/admin/students/" + target.getId() + "/suspend").session(session(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.suspended").value(true));
    assertThat(listings.findByIdAndSchoolDomain(item.getId(), "tulane.edu")).isEmpty();
    mvc.perform(post("/api/admin/students/" + target.getId() + "/unsuspend").session(session(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.suspended").value(false));
    assertThat(listings.findByIdAndSchoolDomain(item.getId(), "tulane.edu")).isPresent();
  }

  @Test void selfChangesAndBadInputsReturnExactMessages() throws Exception {
    Student admin = student("Admin", "admin@tulane.edu", "tulane.edu", Role.ADMIN);
    Student target = student("Target", "target@tulane.edu", "tulane.edu", Role.STUDENT);
    mvc.perform(post("/api/admin/students/" + admin.getId() + "/suspend").session(session(admin)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("You cannot suspend yourself"));
    mvc.perform(put("/api/admin/students/" + admin.getId() + "/role").session(session(admin))
        .contentType("application/json").content("{\"role\":\"STUDENT\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("You cannot change your own role"));
    mvc.perform(put("/api/admin/students/" + target.getId() + "/role").session(session(admin))
        .contentType("application/json").content("{\"role\":\"OWNER\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("role must be ADMIN or STUDENT"));
    mvc.perform(get("/api/admin/students?limit=101").session(session(admin)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("limit must be between 1 and 100"));
    mvc.perform(get("/api/admin/students?q=target").session(session(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.meta.totalItems").value(1))
        .andExpect(jsonPath("$.items[0].email").value("target@tulane.edu"));
  }

  @Test void adminDeletesSuspendedSellerListingWithinSchool() throws Exception {
    Student admin = student("Admin", "admin@tulane.edu", "tulane.edu", Role.ADMIN);
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu", Role.STUDENT);
    Student other = student("Other", "other@usf.edu", "usf.edu", Role.STUDENT);
    Listing local = listing(seller);
    Listing remote = listing(other);
    seller.setSuspended(true);
    students.save(seller);
    mvc.perform(get("/listings/" + local.getId()).session(session(admin))).andExpect(status().isNotFound());
    mvc.perform(delete("/listings/" + remote.getId()).session(session(admin))).andExpect(status().isNotFound());
    mvc.perform(delete("/listings/" + local.getId()).session(session(admin))).andExpect(status().isNoContent());
    assertThat(listings.findById(local.getId())).isEmpty();
  }
}
