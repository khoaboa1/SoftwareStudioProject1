package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import com.Handoff.backend.service.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationServicePinDispatchTest {

  @Mock
  private StudentRepository studentRepository;

  /** Test double that records every call and answers with a fixed result. */
  private static class RecordingSender implements EmailSender {
    private final boolean result;
    private final List<String> calls;
    private final String name;

    RecordingSender(String name, boolean result, List<String> calls) {
      this.name = name;
      this.result = result;
      this.calls = calls;
    }

    @Override
    public boolean send(String to, String pin) {
      calls.add(name + ":" + to + ":" + pin);
      return result;
    }
  }

  private Student student() {
    return new Student("Bob", "bob@tulane.edu", "hashed", null, null);
  }

  @Test
  void generateAndSendPin_stopsAtFirstSenderThatSucceeds() {
    List<String> calls = new ArrayList<>();
    VerificationService service = new VerificationService(studentRepository, List.of(
        new RecordingSender("webhook", false, calls),
        new RecordingSender("resend", true, calls),
        new RecordingSender("smtp", true, calls)));
    Student student = student();

    String pin = service.generateAndSendPin(student);

    assertThat(calls).containsExactly(
        "webhook:bob@tulane.edu:" + pin,
        "resend:bob@tulane.edu:" + pin);
  }

  @Test
  void generateAndSendPin_savesSixDigitPinWithExpiry_evenWhenEverySenderFails() {
    List<String> calls = new ArrayList<>();
    VerificationService service = new VerificationService(studentRepository, List.of(
        new RecordingSender("webhook", false, calls),
        new RecordingSender("smtp", false, calls)));
    Student student = student();

    String pin = service.generateAndSendPin(student);

    assertThat(pin).matches("\\d{6}");
    assertThat(student.getVerificationPin()).isEqualTo(pin);
    assertThat(student.getVerificationPinExpiry()).isNotNull();
    assertThat(calls).hasSize(2);
    verify(studentRepository).save(student);
  }

  @Test
  void generateAndSendPin_worksWithNoSendersConfigured() {
    VerificationService service = new VerificationService(studentRepository, List.of());

    assertThat(service.generateAndSendPin(student())).matches("\\d{6}");
  }

  @Test
  void generateAndSendPin_rejectsNonEduEmailBeforeSending() {
    List<String> calls = new ArrayList<>();
    VerificationService service = new VerificationService(studentRepository, List.of(
        new RecordingSender("webhook", true, calls)));
    Student student = new Student("Bob", "bob@gmail.com", "hashed", null, null);

    assertThatThrownBy(() -> service.generateAndSendPin(student))
        .isInstanceOf(InvalidSignupException.class);
    assertThat(calls).isEmpty();
    verify(studentRepository, never()).save(student);
  }
}
