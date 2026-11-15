package com.Handoff.backend.controller;

import com.Handoff.backend.dto.ResendPinRequest;
import com.Handoff.backend.dto.SignupRequest;
import com.Handoff.backend.dto.VerificationRequest;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.AuthService;
import com.Handoff.backend.service.UniversityEmailValidator;
import com.Handoff.backend.service.VerificationService;
import com.Handoff.backend.service.email.EmailSender;
import com.Handoff.backend.service.auth.PendingLoginStore;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import com.Handoff.backend.service.auth.SupabaseSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthEmailValidationControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SupabaseAuthClient supabase;
    @Mock
    private EmailSender emailSender;

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UniversityEmailValidator validator = new UniversityEmailValidator();
        VerificationService verificationService = new VerificationService(studentRepository, List.of(emailSender),
                validator);
        AuthService authService = new AuthService(studentRepository, verificationService, validator, supabase,
                new PendingLoginStore(), new BCryptPasswordEncoder());
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AuthController(authService, new CurrentStudentResolver(authService), new AuthCookies(false)))
                .build();
    }

    private Student studentWithPin(String email) {
        Student student = new Student("Jane Doe", email, "hashed", null, null);
        student.setId(42L);
        student.setVerificationPin("123456");
        student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(15));
        return student;
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
    void signup_returnsCreatedAndUnverifiedStudent(String email) throws Exception {
        when(studentRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(supabase.signUp(email, "Password123!")).thenReturn(USER_ID);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(42L);
            return student;
        });

        MvcResult result = mockMvc
                .perform(post("/auth/signup").contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SignupRequest("Jane Doe", email, "Password123!"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.verificationPin").doesNotExist()).andReturn();

        assertThat(result.getResponse().getCookie(AuthCookies.ACCESS)).isNull();
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
    void verifyPin_returnsVerifiedStudentAndSetsAuthCookies(String email) throws Exception {
        Student student = studentWithPin(email);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));
        when(studentRepository.save(student)).thenReturn(student);
        when(supabase.verifySignupOtp(email, "123456"))
                .thenReturn(new SupabaseSession("access-token", "refresh-token", 3600, USER_ID));

        MvcResult result = mockMvc
                .perform(post("/auth/verify-pin").contentType("application/json").content(
                        objectMapper.writeValueAsString(new VerificationRequest(email, "123456", "browser-1"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.verified").value(true)).andExpect(jsonPath("$.emailVerified").value(true))
                .andReturn();

        assertThat(result.getResponse().getCookie(AuthCookies.ACCESS).getValue()).isEqualTo("access-token");
        assertThat(student.isDeviceTrusted("browser-1")).isTrue();
        verifyNoInteractions(emailSender);
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
    void resendPin_returnsNoContentAndResendsSupabaseCode(String email) throws Exception {
        Student student = studentWithPin(email);
        when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

        MvcResult result = mockMvc
                .perform(post("/auth/resend-pin").contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ResendPinRequest(email))))
                .andExpect(status().isNoContent()).andExpect(content().string("")).andReturn();

        verify(supabase).resendSignupOtp(email);
        verifyNoInteractions(emailSender);
        assertThat(result.getResponse().getCookie(AuthCookies.ACCESS)).isNull();
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
    void signup_rejectsInvalidEmailWithExistingMessage(String email) throws Exception {
        assertInvalidEmail("/auth/signup", new SignupRequest("Jane Doe", email, "Password123!"));
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
    void verifyPin_rejectsInvalidEmailWithExistingMessage(String email) throws Exception {
        assertInvalidEmail("/auth/verify-pin", new VerificationRequest(email, "123456", "browser-1"));
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
    void resendPin_rejectsInvalidEmailWithExistingMessage(String email) throws Exception {
        assertInvalidEmail("/auth/resend-pin", new ResendPinRequest(email));
    }

    private void assertInvalidEmail(String endpoint, Object request) throws Exception {
        MvcResult result = mockMvc
                .perform(post(endpoint).contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"message\":\"Use a valid .edu school email\"}")).andReturn();

        assertThat(result.getResponse().getCookie(AuthCookies.ACCESS)).isNull();
        verifyNoInteractions(studentRepository, supabase, emailSender);
    }
}
