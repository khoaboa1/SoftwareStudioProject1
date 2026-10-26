package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

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
  private PasswordEncoder passwordEncoder;
  @Mock
  private EmailSender emailSender;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    UniversityEmailValidator validator = new UniversityEmailValidator();
    VerificationService verificationService =
        new VerificationService(studentRepository, List.of(emailSender), validator);
    authService = new AuthService(studentRepository, passwordEncoder, verificationService, validator);
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
  void signup_acceptsEmailAndSendsPinWithoutNormalizing(String email) {
    when(studentRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
    when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Student student = authService.signup("Jane Doe", email, "Password123!");

    assertThat(student.getEmail()).isEqualTo(email);
    assertThat(student.getPasswordHash()).isEqualTo("hashed");
    assertThat(student.isVerified()).isFalse();
    assertThat(student.getVerificationPin()).matches("\\d{6}");
    verify(studentRepository).findByEmail(email);
    verify(emailSender).send(email, student.getVerificationPin());
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
  void signup_rejectsEmailBeforePasswordValidationOrSideEffects(String email) {
    assertThatThrownBy(() -> authService.signup("Jane Doe", email, null))
        .isExactlyInstanceOf(InvalidSignupException.class)
        .hasMessage("Use a valid .edu school email");

    verifyNoInteractions(studentRepository, passwordEncoder, emailSender);
  }

  @Test
  void signup_validatesNameBeforeEmail() {
    assertThatThrownBy(() -> authService.signup(" ", "jane@gmail.com", "Password123!"))
        .isExactlyInstanceOf(InvalidSignupException.class)
        .hasMessage("Full name is required");

    verifyNoInteractions(studentRepository, passwordEncoder, emailSender);
  }

  @Test
  void signup_validatesPasswordAfterAcceptedEmail() {
    assertThatThrownBy(() -> authService.signup("Jane Doe", "jane@tulane.edu", "short"))
        .isExactlyInstanceOf(InvalidSignupException.class)
        .hasMessage("Password must be at least 8 characters");

    verifyNoInteractions(studentRepository, passwordEncoder, emailSender);
  }
}
