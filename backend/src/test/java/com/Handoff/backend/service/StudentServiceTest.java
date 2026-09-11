package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

  @Mock
  private StudentRepository studentRepository;

  @Test
  void getAllStudents_returnsAllStudentsFromRepository() {
    Student sarah = new Student(
        "Sarah", "sarah@tulane.edu", "hashed",
        List.of("Desk Lamp"), "Wall Residence Hall");
    when(studentRepository.findAll()).thenReturn(List.of(sarah));

    StudentService service = new StudentService(studentRepository);

    assertThat(service.getAllStudents()).containsExactly(sarah);
  }
}
