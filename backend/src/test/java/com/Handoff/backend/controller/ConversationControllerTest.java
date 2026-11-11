package com.Handoff.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ConversationRepository;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private StudentRepository studentRepository;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private ListingRepository listingRepository;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    sessions.clear();
    conversationRepository.deleteAll();
    listingRepository.deleteAll();
    profileRepository.deleteAll();
    studentRepository.deleteAll();
  }

  private MockHttpSession loginAsNewStudent(String email) throws Exception {
    String signupBody = objectMapper.writeValueAsString(
        new SignupRequest("Jane Doe", email, "Password123!"));
    mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
        .andExpect(status().isCreated());
    Student student = studentRepository.findByEmail(email).orElseThrow();
    VerificationRequest verifyBody = new VerificationRequest(email, student.getVerificationPin(), "test-device");
    MvcResult verifyResult = mockMvc.perform(post("/auth/verify-pin")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(verifyBody)))
        .andExpect(status().isOk()).andReturn();
    HttpSession session = verifyResult.getRequest().getSession(false);
    sessions.put(email, (MockHttpSession) session);
    return (MockHttpSession) session;
  }

  private Student student(String name, String email, String domain) throws Exception {
    loginAsNewStudent(email);
    Student student = studentRepository.findByEmail(email).orElseThrow();
    student.setStudentName(name);
    studentRepository.save(student);
    profileRepository.save(new Profile(student, name, "CS", null, domain));
    return student;
  }

  private Listing listing(Student seller) {
    return listingRepository.save(new Listing("Desk Lamp", "Works great", BigDecimal.TEN,
        Condition.GOOD, Category.FURNITURE, seller));
  }

  @Test
  void start_firstTime_returns201WithBody() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Student buyer = student("Buyer", "buyer@tulane.edu", "tulane.edu");
    Listing listing = listing(seller);
    MockHttpSession session = loginSession("buyer@tulane.edu");

    mockMvc.perform(post("/listings/{id}/conversation", listing.getId()).session(session))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.listingId").value(listing.getId()))
        .andExpect(jsonPath("$.itemName").value("Desk Lamp"))
        .andExpect(jsonPath("$.buyerId").value(buyer.getId()))
        .andExpect(jsonPath("$.buyerName").value("Buyer"))
        .andExpect(jsonPath("$.sellerId").value(seller.getId()))
        .andExpect(jsonPath("$.sellerName").value("Seller"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  private MockHttpSession loginSession(String email) {
    return sessions.get(email);
  }

  private final java.util.Map<String, MockHttpSession> sessions = new java.util.HashMap<>();

  @Test
  void start_secondTime_returns200SameId() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    student("Buyer", "buyer@tulane.edu", "tulane.edu");
    Listing listing = listing(seller);
    MockHttpSession session = loginSession("buyer@tulane.edu");
    MvcResult first = mockMvc.perform(post("/listings/{id}/conversation", listing.getId()).session(session))
        .andExpect(status().isCreated()).andReturn();
    long firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asLong();
    mockMvc.perform(post("/listings/{id}/conversation", listing.getId()).session(session))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(firstId));
    assertThat(conversationRepository.count()).isEqualTo(1);
  }

  @Test
  void start_notLoggedIn_returns401() throws Exception {
    mockMvc.perform(post("/listings/1/conversation"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("You must be logged in to message a seller"));
  }

  @Test
  void start_ownListing_returns403() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    Listing listing = listing(seller);
    mockMvc.perform(post("/listings/{id}/conversation", listing.getId())
            .session(loginSession("seller@tulane.edu")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("You cannot message yourself about your own listing"));
  }

  @Test
  void start_soldListing_returns409() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    student("Buyer", "buyer@tulane.edu", "tulane.edu");
    Listing listing = listing(seller);
    listing.setStatus(ListingStatus.SOLD);
    listingRepository.save(listing);
    mockMvc.perform(post("/listings/{id}/conversation", listing.getId())
            .session(loginSession("buyer@tulane.edu")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("This item has already been sold"));
  }

  @Test
  void start_otherSchoolListing_returns404() throws Exception {
    Student seller = student("Seller", "seller@usf.edu", "usf.edu");
    student("Buyer", "buyer@tulane.edu", "tulane.edu");
    Listing listing = listing(seller);
    mockMvc.perform(post("/listings/{id}/conversation", listing.getId())
            .session(loginSession("buyer@tulane.edu")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Listing not found"));
  }

  @Test
  void start_missingListing_returns404() throws Exception {
    student("Buyer", "buyer@tulane.edu", "tulane.edu");
    mockMvc.perform(post("/listings/999999/conversation").session(loginSession("buyer@tulane.edu")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Listing not found"));
  }

  @Test
  void start_requesterWithoutProfile_returns404ProfileNotFound() throws Exception {
    MockHttpSession session = loginAsNewStudent("buyer@tulane.edu");
    mockMvc.perform(post("/listings/999999/conversation").session(session))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Profile not found"));
  }

  @Test
  void deleteListing_withConversation_succeeds() throws Exception {
    Student seller = student("Seller", "seller@tulane.edu", "tulane.edu");
    student("Buyer", "buyer@tulane.edu", "tulane.edu");
    Listing listing = listing(seller);
    mockMvc.perform(post("/listings/{id}/conversation", listing.getId())
            .session(loginSession("buyer@tulane.edu"))).andExpect(status().isCreated());
    mockMvc.perform(delete("/listings/{id}", listing.getId())
            .session(loginSession("seller@tulane.edu"))).andExpect(status().isNoContent());
    assertThat(conversationRepository.count()).isZero();
  }
}
