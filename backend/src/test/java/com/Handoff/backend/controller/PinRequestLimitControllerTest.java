package com.Handoff.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.support.FakeSupabaseAuthClient;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "auth.pin.resend-cooldown-seconds=60", "auth.pin.max-sends-per-hour=5" })
class PinRequestLimitControllerTest {
    private static final String EMAIL = "limits@tulane.edu";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private FakeSupabaseAuthClient supabase;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        studentRepository.deleteAll();
        supabase.reset();
    }

    private void resend() throws Exception {
        mockMvc.perform(
                post("/auth/resend-pin").contentType("application/json").content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void aSecondResendInsideTheCooldownGets429WithRetryAfter() throws Exception {
        studentRepository.save(new Student("Limits", EMAIL, "hashed", null, null));
        resend();
        mockMvc.perform(
                post("/auth/resend-pin").contentType("application/json").content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.message").value("Please wait 60 seconds before requesting a new PIN."));
    }

    @Test
    void theHourlyCapGets429() throws Exception {
        Student student = new Student("Limits", EMAIL, "hashed", null, null);
        student.setPinSendWindowStart(java.time.LocalDateTime.now().minusMinutes(10));
        student.setPinSendCount(5);
        studentRepository.save(student);
        mockMvc.perform(
                post("/auth/resend-pin").contentType("application/json").content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Too many PIN requests. Please try again in 50 minutes."));
    }

    @Test
    void signingUpAgainInsideTheCooldownKeepsTheFirstPin() throws Exception {
        String body = "{\"studentName\":\"Limits\",\"email\":\"" + EMAIL + "\",\"password\":\"Password123!\"}";
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        String firstPin = studentRepository.findByEmail(EMAIL).orElseThrow().getVerificationPin();
        mockMvc.perform(post("/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        Student student = studentRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(student.getVerificationPin()).isEqualTo(firstPin);
        assertThat(student.getPinSendCount()).isEqualTo(1);
    }
}
