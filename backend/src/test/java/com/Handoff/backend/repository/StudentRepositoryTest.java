package com.Handoff.backend.repository;

import com.Handoff.backend.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StudentRepositoryTest {

  @Autowired
  private StudentRepository studentRepository;

  @Test
  void findByEmail_returnsMatchingStudent() {
    studentRepository.save(new Student(
        "Sarah", "sarah@tulane.edu", "hashed-password",
        List.of("Desk Lamp"), "Wall Residence Hall"));

    Optional<Student> found = studentRepository.findByEmail("sarah@tulane.edu");

    assertThat(found).isPresent();
    assertThat(found.get().getStudentName()).isEqualTo("Sarah");
  }

  @Test
  void findByEmail_returnsEmpty_whenNoMatch() {
    Optional<Student> found = studentRepository.findByEmail("nobody@tulane.edu");

    assertThat(found).isEmpty();
  }
}
