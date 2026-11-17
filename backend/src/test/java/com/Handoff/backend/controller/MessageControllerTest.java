package com.Handoff.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Handoff.backend.dto.LoginRequest;
import com.Handoff.backend.dto.SendMessageRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Category;
import com.Handoff.backend.model.Condition;
import com.Handoff.backend.model.Conversation;
import com.Handoff.backend.model.Listing;
import com.Handoff.backend.model.ListingStatus;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ConversationRepository;
import com.Handoff.backend.repository.ListingRepository;
import com.Handoff.backend.repository.MessageRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.support.FakeSupabaseAuthClient;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {
    private static final String SEND_LOGIN = "You must be logged in to send messages";
    private static final String VIEW_LOGIN = "You must be logged in to view messages";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FakeSupabaseAuthClient supabase;

    private final Map<String, Cookie> sessions = new HashMap<>();

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        sessions.clear();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        listingRepository.deleteAll();
        profileRepository.deleteAll();
        studentRepository.deleteAll();
        supabase.reset();
    }

    private Student student(String name, String email) throws Exception {
        String signupBody = objectMapper.writeValueAsString(new SignupRequest("Jane Doe", email, "Password123!"));
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(signupBody))
                .andExpect(status().isCreated());
        String pin = studentRepository.findByEmail(email).orElseThrow().getVerificationPin();
        VerificationRequest verifyBody = new VerificationRequest(email, pin, "test-device");
        mockMvc.perform(post("/auth/verify-pin").contentType("application/json")
                .content(objectMapper.writeValueAsString(verifyBody))).andExpect(status().isOk());
        MvcResult loginResult = mockMvc
                .perform(post("/auth/login").contentType("application/json").content(
                        objectMapper.writeValueAsString(new LoginRequest(email, "Password123!", "test-device"))))
                .andExpect(status().isOk()).andReturn();
        sessions.put(email, loginResult.getResponse().getCookie(AuthCookies.ACCESS));
        Student student = studentRepository.findByEmail(email).orElseThrow();
        student.setStudentName(name);
        studentRepository.save(student);
        profileRepository.save(new Profile(student, name, "CS", null, "tulane.edu"));
        return student;
    }

    private Conversation conversation(Student seller, Student buyer) {
        Listing listing = listingRepository.save(
                new Listing("Desk Lamp", "Works great", BigDecimal.TEN, Condition.GOOD, Category.FURNITURE, seller));
        return conversationRepository.save(new Conversation(listing, buyer));
    }

    private Conversation soldConversation(Student seller, Student buyer) {
        Conversation conversation = conversation(seller, buyer);
        Listing listing = conversation.getListing();
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);
        return conversation;
    }

    private String body(String content) {
        return objectMapper.writeValueAsString(new SendMessageRequest(content));
    }

    private void send(Conversation conversation, String email, String content) throws Exception {
        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get(email))
                .contentType("application/json").content(body(content))).andExpect(status().isCreated());
    }

    @Test
    void send_asBuyer_returns201WithBody() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(
                post("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("buyer@tulane.edu"))
                        .contentType("application/json").content(body("Is it still available?")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.conversationId").value(conversation.getId()))
                .andExpect(jsonPath("$.senderId").value(buyer.getId()))
                .andExpect(jsonPath("$.senderName").value("Buyer"))
                .andExpect(jsonPath("$.content").value("Is it still available?"))
                .andExpect(jsonPath("$.createdAt").exists());
        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    void send_asSeller_returns201() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                .cookie(sessions.get("seller@tulane.edu")).contentType("application/json").content(body("Yes!")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senderId").value(seller.getId()))
                .andExpect(jsonPath("$.senderName").value("Seller"));
    }

    @Test
    void send_trimsContent() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                .cookie(sessions.get("buyer@tulane.edu")).contentType("application/json").content(body("  hi  ")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.content").value("hi"));
    }

    @Test
    void send_blank_returns400WithMessage() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                .cookie(sessions.get("buyer@tulane.edu")).contentType("application/json").content(body("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Message content is required"));
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void send_tooLong_returns400() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(
                post("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("buyer@tulane.edu"))
                        .contentType("application/json").content(body("a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Message content must be at most 2000 characters"));
    }

    @Test
    void send_asOutsider_returns403() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        student("Other", "other@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                .cookie(sessions.get("other@tulane.edu")).contentType("application/json").content(body("hi")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this conversation"));
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void send_unknownConversation_returns404() throws Exception {
        student("Buyer", "buyer@tulane.edu");

        mockMvc.perform(post("/api/conversations/999999/messages").cookie(sessions.get("buyer@tulane.edu"))
                .contentType("application/json").content(body("hi"))).andExpect(status().isNotFound());
    }

    @Test
    void send_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(post("/api/conversations/1/messages").contentType("application/json").content(body("hi")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(SEND_LOGIN));
    }

    @Test
    void history_returnsMessagesOldestFirst_forBothParticipants() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);
        send(conversation, "buyer@tulane.edu", "first");
        send(conversation, "seller@tulane.edu", "second");
        send(conversation, "buyer@tulane.edu", "third");

        for (String email : new String[] { "buyer@tulane.edu", "seller@tulane.edu" }) {
            mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get(email)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].content").value("first"))
                    .andExpect(jsonPath("$[0].senderName").value("Buyer"))
                    .andExpect(jsonPath("$[0].createdAt").exists()).andExpect(jsonPath("$[1].content").value("second"))
                    .andExpect(jsonPath("$[1].senderName").value("Seller"))
                    .andExpect(jsonPath("$[2].content").value("third"));
        }
    }

    @Test
    void history_withAfterCursor_returnsOnlyNewerMessages() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);
        send(conversation, "buyer@tulane.edu", "first");
        send(conversation, "seller@tulane.edu", "second");
        long firstId = messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(conversation.getId()).get(0)
                .getId();

        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId()).param("after", "" + firstId)
                .cookie(sessions.get("buyer@tulane.edu"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].content").value("second"));
    }

    @Test
    void history_emptyConversation_returnsEmptyArray() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = conversation(seller, buyer);

        mockMvc.perform(
                get("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("buyer@tulane.edu")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void history_asOutsider_returns403() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        student("Other", "other@tulane.edu");
        Conversation conversation = conversation(seller, buyer);
        send(conversation, "buyer@tulane.edu", "private");

        mockMvc.perform(
                get("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("other@tulane.edu")))
                .andExpect(status().isForbidden());
    }

    @Test
    void history_unknownConversation_returns404() throws Exception {
        student("Buyer", "buyer@tulane.edu");

        mockMvc.perform(get("/api/conversations/999999/messages").cookie(sessions.get("buyer@tulane.edu")))
                .andExpect(status().isNotFound());
    }

    @Test
    void history_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get("/api/conversations/1/messages")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(VIEW_LOGIN));
    }

    @Test
    void send_onSoldListing_stillWorksForBuyerAndSeller() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = soldConversation(seller, buyer);

        mockMvc.perform(
                post("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("buyer@tulane.edu"))
                        .contentType("application/json").content(body("Still meeting at 5?")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senderId").value(buyer.getId()));

        mockMvc.perform(
                post("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("seller@tulane.edu"))
                        .contentType("application/json").content(body("Yes, see you")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senderId").value(seller.getId()));

        assertThat(messageRepository.count()).isEqualTo(2);
    }

    @Test
    void history_onSoldListing_stillWorksForBothParticipants() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = soldConversation(seller, buyer);
        send(conversation, "buyer@tulane.edu", "first");
        send(conversation, "seller@tulane.edu", "second");

        for (String email : new String[] { "buyer@tulane.edu", "seller@tulane.edu" }) {
            mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get(email)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].content").value("first"))
                    .andExpect(jsonPath("$[1].content").value("second"));
        }
    }

    @Test
    void history_onSoldListing_withAfterCursor_returnsOnlyNewerMessages() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        Conversation conversation = soldConversation(seller, buyer);
        send(conversation, "buyer@tulane.edu", "first");
        send(conversation, "seller@tulane.edu", "second");
        long firstId = messageRepository.findByConversation_IdOrderByCreatedAtAscIdAsc(conversation.getId()).get(0)
                .getId();

        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId()).param("after", "" + firstId)
                .cookie(sessions.get("seller@tulane.edu"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].content").value("second"));
    }

    @Test
    void send_onSoldListing_asOutsider_returns403() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        student("Other", "other@tulane.edu");
        Conversation conversation = soldConversation(seller, buyer);

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                .cookie(sessions.get("other@tulane.edu")).contentType("application/json").content(body("hi")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this conversation"));
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void history_onSoldListing_asOutsider_returns403() throws Exception {
        Student seller = student("Seller", "seller@tulane.edu");
        Student buyer = student("Buyer", "buyer@tulane.edu");
        student("Other", "other@tulane.edu");
        Conversation conversation = soldConversation(seller, buyer);
        send(conversation, "buyer@tulane.edu", "private");

        mockMvc.perform(
                get("/api/conversations/{id}/messages", conversation.getId()).cookie(sessions.get("other@tulane.edu")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this conversation"));
    }
}
