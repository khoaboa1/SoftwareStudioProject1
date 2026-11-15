package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.auth.PendingLoginStore;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import com.Handoff.backend.service.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceEmailValidationTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SupabaseAuthClient supabase;
    @Mock
    private EmailSender emailSender;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private AuthService authService;

    @BeforeEach
    void setUp() {
        UniversityEmailValidator validator = new UniversityEmailValidator();
        VerificationService verificationService = new VerificationService(studentRepository, List.of(emailSender),
                validator);
        authService = new AuthService(studentRepository, verificationService, validator, supabase,
                new PendingLoginStore(), new BCryptPasswordEncoder());
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
    void signup_acceptsEmailAndCreatesSupabaseUserWithoutNormalizing(String email) {
        when(studentRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(supabase.adminCreateUnconfirmedUser(email, "Password123!")).thenReturn(USER_ID);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Student student = authService.signup("Jane Doe", email, "Password123!");
        assertThat(student.getEmail()).isEqualTo(email);
        assertThat(student.getSupabaseUserId()).isEqualTo(USER_ID);
        assertThat(student.isVerified()).isFalse();
        assertThat(student.getVerificationPin()).matches("\\d{6}");
        verify(studentRepository).findByEmail(email);
        verify(emailSender).send(email, student.getVerificationPin());
    }

    @ParameterizedTest
    @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
    void signup_rejectsEmailBeforePasswordValidationOrSideEffects(String email) {
        assertThatThrownBy(() -> authService.signup("Jane Doe", email, null))
                .isExactlyInstanceOf(InvalidSignupException.class).hasMessage("Use a valid .edu school email");

        verifyNoInteractions(studentRepository, supabase, emailSender);
    }

    @Test
    void signup_validatesNameBeforeEmail() {
        assertThatThrownBy(() -> authService.signup(" ", "jane@gmail.com", "Password123!"))
                .isExactlyInstanceOf(InvalidSignupException.class).hasMessage("Full name is required");

        verifyNoInteractions(studentRepository, supabase, emailSender);
    }

    @Test
    void signup_validatesPasswordAfterAcceptedEmail() {
        assertThatThrownBy(() -> authService.signup("Jane Doe", "jane@tulane.edu", "short"))
                .isExactlyInstanceOf(InvalidSignupException.class).hasMessage("Password must be at least 8 characters");

        verifyNoInteractions(studentRepository, supabase, emailSender);
    }
}
