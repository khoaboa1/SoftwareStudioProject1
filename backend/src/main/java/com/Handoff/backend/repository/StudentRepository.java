package com.Handoff.backend.repository;

import com.Handoff.backend.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, Long> {
  Optional<Student> findByEmail(String email);
  Optional<Student> findBySupabaseUserId(UUID supabaseUserId);
  List<Student> findByVerifiedFalseAndCreatedAtBefore(LocalDateTime cutoff);
}
