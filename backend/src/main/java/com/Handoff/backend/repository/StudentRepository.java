package com.Handoff.backend.repository;

import com.Handoff.backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
  Optional<Student> findByEmail(String email);
  List<Student> findByVerifiedFalseAndCreatedAtBefore(LocalDateTime cutoff);
}
