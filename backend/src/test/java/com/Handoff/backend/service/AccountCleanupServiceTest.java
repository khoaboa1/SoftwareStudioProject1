package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AccountCleanupServiceTest {

  @Autowired
  private StudentRepository studentRepository;

  @Autowired
  private AccountCleanupService accountCleanupService;

  @BeforeEach
  void setUp() {
    studentRepository.deleteAll();
  }

  @Test
  void cleanupExpiredUnverifiedAccounts_deletesOnlyUnverifiedAccountsOlderThan30Minutes() {
    // 1. Expired unverified student (created 35 minutes ago)
    Student expiredUnverified = new Student("Expired", "expired@tulane.edu", "hash123", null, null);
    expiredUnverified.setVerified(false);
    expiredUnverified.setCreatedAt(LocalDateTime.now().minusMinutes(35));
    studentRepository.save(expiredUnverified);

    // 2. Recent unverified student (created 10 minutes ago)
    Student recentUnverified = new Student("Recent", "recent@tulane.edu", "hash123", null, null);
    recentUnverified.setVerified(false);
    recentUnverified.setCreatedAt(LocalDateTime.now().minusMinutes(10));
    studentRepository.save(recentUnverified);

    // 3. Verified student (created 50 minutes ago)
    Student verifiedOld = new Student("Verified", "verified@tulane.edu", "hash123", null, null);
    verifiedOld.setVerified(true);
    verifiedOld.setCreatedAt(LocalDateTime.now().minusMinutes(50));
    studentRepository.save(verifiedOld);

    int deletedCount = accountCleanupService.cleanupExpiredUnverifiedAccounts();

    assertThat(deletedCount).isEqualTo(1);
    assertThat(studentRepository.findByEmail("expired@tulane.edu")).isEmpty();
    assertThat(studentRepository.findByEmail("recent@tulane.edu")).isPresent();
    assertThat(studentRepository.findByEmail("verified@tulane.edu")).isPresent();
  }
}
