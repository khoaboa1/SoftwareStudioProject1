package com.Handoff.backend.config;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

  static final String SEED_PASSWORD = "Password123!";

  private final StudentRepository studentRepository;
  private final PasswordEncoder passwordEncoder;

  public DataSeeder(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
    this.studentRepository = studentRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    if (studentRepository.count() > 0) {
      return;
    }
    String hashedPassword = passwordEncoder.encode(SEED_PASSWORD);
    studentRepository.saveAll(List.of(
        new Student("Sarah", "sarah@tulane.edu", hashedPassword,
            List.of("Desk Lamp", "Mini Fridge"), "Wall Residence Hall"),
        new Student("Alex", "alex@tulane.edu", hashedPassword,
            List.of("Study Desk", "Office Chair"), "Aron Residences"),
        new Student("Chloe", "chloe@tulane.edu", hashedPassword,
            List.of("Bedside Fan", "Storage Bins"), "Weatherhead"),
        new Student("Brian", "brian@tulane.edu", hashedPassword, null, null)));
  }
}
