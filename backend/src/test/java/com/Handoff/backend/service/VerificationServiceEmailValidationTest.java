package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceEmailValidationTest {

  @Mock
  private StudentRepository studentRepository;
  @Mock
  private EmailSender emailSender;

  private VerificationService verificationService;

  @BeforeEach
  void setUp() {
    verificationService = new VerificationService(
        studentRepository, List.of(emailSender), new UniversityEmailValidator());
  }

  private Student studentWithPin(String email) {
    Student student = new Student("Jane Doe", email, "hashed", null, null);
    student.setVerificationPin("123456");
    student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(15));
    return student;
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
  void generateAndSendPin_acceptsEmailAndPersistsPinBeforeDispatch(String email) {
    Student student = studentWithPin(email);
    LocalDateTime before = LocalDateTime.now();

    String pin = verificationService.generateAndSendPin(student);

    assertThat(pin).matches("\\d{6}");
    assertThat(student.getVerificationPin()).isEqualTo(pin);
    assertThat(student.getVerificationPinExpiry())
        .isBetween(before.plusMinutes(15), LocalDateTime.now().plusMinutes(15));
    var order = org.mockito.Mockito.inOrder(studentRepository, emailSender);
    order.verify(studentRepository).save(student);
    order.verify(emailSender).send(email, pin);
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
  void generateAndSendPin_rejectsEmailWithoutMutatingStudentOrDispatching(String email) {
    Student student = studentWithPin(email);
    LocalDateTime expiry = student.getVerificationPinExpiry();

    assertThatThrownBy(() -> verificationService.generateAndSendPin(student))
        .isExactlyInstanceOf(InvalidSignupException.class)
        .hasMessage("Only .edu school emails can receive a verification PIN");

    assertThat(student.getVerificationPin()).isEqualTo("123456");
    assertThat(student.getVerificationPinExpiry()).isEqualTo(expiry);
    verifyNoInteractions(studentRepository, emailSender);
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
  void verifyPin_acceptsEmailAndPreservesVerificationBehavior(String email) {
    Student student = studentWithPin(email);
    when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));
    when(studentRepository.save(student)).thenReturn(student);

    Student verified = verificationService.verifyPin(email, "123456", "browser-1");

    assertThat(verified).isSameAs(student);
    assertThat(verified.getEmail()).isEqualTo(email);
    assertThat(verified.isVerified()).isTrue();
    assertThat(verified.isEmailVerified()).isTrue();
    assertThat(verified.getVerificationPin()).isNull();
    assertThat(verified.getVerificationPinExpiry()).isNull();
    assertThat(verified.isDeviceTrusted("browser-1")).isTrue();
    verify(studentRepository).findByEmail(email);
    verify(studentRepository).save(student);
    verifyNoInteractions(emailSender);
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
  void verifyPin_rejectsEmailBeforePinValidationOrSideEffects(String email) {
    assertThatThrownBy(() -> verificationService.verifyPin(email, null, "browser-1"))
        .isExactlyInstanceOf(InvalidVerificationPinException.class)
        .hasMessage("Use a valid .edu school email");

    verifyNoInteractions(studentRepository, emailSender);
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#acceptedEmails")
  void resendPin_acceptsEmailAndReplacesExpiredPin(String email) {
    Student student = studentWithPin(email);
    student.setVerificationPin("old-pin");
    student.setVerificationPinExpiry(LocalDateTime.now().minusMinutes(1));
    when(studentRepository.findByEmail(email)).thenReturn(Optional.of(student));

    verificationService.resendPin(email);

    assertThat(student.getVerificationPin()).matches("\\d{6}");
    assertThat(student.getVerificationPinExpiry()).isAfter(LocalDateTime.now());
    assertThat(student.isVerified()).isFalse();
    verify(studentRepository).findByEmail(email);
    verify(studentRepository).save(student);
    verify(emailSender).send(email, student.getVerificationPin());
  }

  @ParameterizedTest
  @MethodSource("com.Handoff.backend.support.UniversityEmailTestCases#rejectedEmails")
  void resendPin_rejectsEmailBeforeSideEffects(String email) {
    assertThatThrownBy(() -> verificationService.resendPin(email))
        .isExactlyInstanceOf(InvalidSignupException.class)
        .hasMessage("Use a valid .edu school email");

    verifyNoInteractions(studentRepository, emailSender);
  }
}
