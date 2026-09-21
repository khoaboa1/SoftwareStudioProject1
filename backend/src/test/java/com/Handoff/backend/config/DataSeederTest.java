package com.Handoff.backend.config;

import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

  @Mock
  private StudentRepository studentRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Test
  void run_seedsFourStudents_whenRepositoryEmpty() throws Exception {
    when(studentRepository.count()).thenReturn(0L);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed");

    new DataSeeder(studentRepository, passwordEncoder).run();

    verify(studentRepository).saveAll(argThatHasSize(4));
  }

  @Test
  void run_doesNothing_whenRepositoryAlreadyHasData() throws Exception {
    when(studentRepository.count()).thenReturn(4L);

    new DataSeeder(studentRepository, passwordEncoder).run();

    verify(studentRepository, never()).saveAll(any());
  }

  private static Iterable<com.Handoff.backend.model.Student> argThatHasSize(int expectedSize) {
    return org.mockito.ArgumentMatchers.argThat(students -> {
      int count = 0;
      for (var ignored : students) {
        count++;
      }
      return count == expectedSize;
    });
  }
}
