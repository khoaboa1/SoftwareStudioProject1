package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountCleanupService {

  private static final Logger logger = LoggerFactory.getLogger(AccountCleanupService.class);
  public static final long EXPIRATION_MINUTES = 30;

  private final StudentRepository studentRepository;

  public AccountCleanupService(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
  }

  @Scheduled(fixedRate = 60000)
  @Transactional
  public int cleanupExpiredUnverifiedAccounts() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);
    List<Student> expiredStudents = studentRepository.findByVerifiedFalseAndCreatedAtBefore(cutoff);
    if (!expiredStudents.isEmpty()) {
      logger.info("[ACCOUNT CLEANUP] Purging {} unverified zombie account(s) older than {} minutes",
          expiredStudents.size(), EXPIRATION_MINUTES);
      studentRepository.deleteAll(expiredStudents);
      return expiredStudents.size();
    }
    return 0;
  }
}
