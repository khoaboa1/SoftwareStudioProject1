package com.Handoff.backend.service;

import com.Handoff.backend.dto.LoginResponse;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  private final StudentRepository studentRepository;
  private final PasswordEncoder passwordEncoder;
  private final VerificationService verificationService;

  public AuthService(StudentRepository studentRepository,
                     PasswordEncoder passwordEncoder,
                     VerificationService verificationService) {
    this.studentRepository = studentRepository;
    this.passwordEncoder = passwordEncoder;
    this.verificationService = verificationService;
  }

  public Student signup(String studentName, String email, String password) {
    if (studentName == null || studentName.isBlank()) {
      throw new InvalidSignupException("Full name is required");
    }
    if (email == null || !EDU_EMAIL_PATTERN.matcher(email).matches()) {
      throw new InvalidSignupException("Use a valid .edu school email");
    }
    if (password == null || password.length() < 8) {
      throw new InvalidSignupException("Password must be at least 8 characters");
    }

    Optional<Student> existingOpt = studentRepository.findByEmail(email);
    if (existingOpt.isPresent()) {
      Student existing = existingOpt.get();
      if (existing.isVerified()) {
        throw new EmailAlreadyRegisteredException("Email already registered");
      }

      // If unverified account is older than 30 minutes, purge zombie account
      if (existing.getCreatedAt() != null && existing.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
        studentRepository.delete(existing);
      } else {
        // Within 30 minutes: refresh account, send fresh PIN, invalidate old PIN
        existing.setStudentName(studentName);
        existing.setPasswordHash(passwordEncoder.encode(password));
        existing.setCreatedAt(LocalDateTime.now());
        existing = studentRepository.save(existing);
        verificationService.generateAndSendPin(existing);
        return existing;
      }
    }

    Student student = new Student(studentName, email, passwordEncoder.encode(password), null, null);
    student.setCreatedAt(LocalDateTime.now());
    student = studentRepository.save(student);

    // Generate and send the initial 6-digit PIN for verification
    verificationService.generateAndSendPin(student);

    return student;
  }

  public LoginResponse login(String email, String password, String deviceId) {
    Student student = studentRepository.findByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);

    if (student.getPasswordHash() == null) {
      throw new InvalidCredentialsException("This account has no password set.");
    }
    if (!passwordEncoder.matches(password, student.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    // Check verification status
    if (!student.isVerified()) {
      // If unverified and older than 30 minutes, purge zombie account
      if (student.getCreatedAt() != null && student.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
        studentRepository.delete(student);
        throw new InvalidCredentialsException("Verification expired after 30 minutes. Please sign up again.");
      }

      // If unverified and within 30 minutes: send a new verification email with fresh PIN
      verificationService.generateAndSendPin(student);
      return LoginResponse.requiresPin(
          student.getEmail(),
          "Account is unverified. A new verification PIN has been sent to your school email."
      );
    }

    // User is verified: they won't be asked for a PIN when signing in
    if (deviceId != null && !deviceId.isBlank()) {
      student.trustDevice(deviceId);
      studentRepository.save(student);
    }

    return LoginResponse.success(student);
  }

  public LoginResponse login(String email, String password) {
    return login(email, password, null);
  }

  public Optional<Student> findById(Long id) {
    return studentRepository.findById(id);
  }
}
