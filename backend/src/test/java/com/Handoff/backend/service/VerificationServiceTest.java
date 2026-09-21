package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class VerificationServiceTest {

  @Autowired
  private StudentRepository studentRepository;

  @Autowired
  private VerificationService verificationService;

  @BeforeEach
  void setUp() {
    studentRepository.deleteAll();
  }

  @Test
  void verifyPin_marksAccountVerifiedAndClearsPin() {
    Student student = new Student("Bob", "bob@tulane.edu", "pass12345", null, null);
    student.setVerified(false);
    student.setVerificationPin("654321");
    student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(15));
    studentRepository.save(student);

    Student verifiedStudent = verificationService.verifyPin("bob@tulane.edu", "654321", "test-device");

    assertThat(verifiedStudent.isVerified()).isTrue();
    assertThat(verifiedStudent.isEmailVerified()).isTrue();
    assertThat(verifiedStudent.getVerificationPin()).isNull();
    assertThat(verifiedStudent.getVerificationPinExpiry()).isNull();
    assertThat(verifiedStudent.isDeviceTrusted("test-device")).isTrue();

    Student reloaded = studentRepository.findByEmail("bob@tulane.edu").orElseThrow();
    assertThat(reloaded.isVerified()).isTrue();
  }

  @Test
  void verifyPin_throwsException_whenPinIncorrect() {
    Student student = new Student("Bob", "bob@tulane.edu", "pass12345", null, null);
    student.setVerified(false);
    student.setVerificationPin("654321");
    student.setVerificationPinExpiry(LocalDateTime.now().plusMinutes(15));
    studentRepository.save(student);

    assertThatThrownBy(() -> verificationService.verifyPin("bob@tulane.edu", "111111", null))
        .isInstanceOf(InvalidVerificationPinException.class)
        .hasMessageContaining("Incorrect verification PIN");

    Student reloaded = studentRepository.findByEmail("bob@tulane.edu").orElseThrow();
    assertThat(reloaded.isVerified()).isFalse();
  }
}
