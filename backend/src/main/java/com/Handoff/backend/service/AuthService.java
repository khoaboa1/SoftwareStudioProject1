package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  private final StudentRepository studentRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
    this.studentRepository = studentRepository;
    this.passwordEncoder = passwordEncoder;
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
    if (studentRepository.findByEmail(email).isPresent()) {
      throw new EmailAlreadyRegisteredException("Email already registered");
    }
    Student student = new Student(studentName, email, passwordEncoder.encode(password), null, null);
    return studentRepository.save(student);
  }

  public Student login(String email, String password) {
    Student student = studentRepository.findByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(password, student.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    return student;
  }

  public Optional<Student> findById(Long id) {
    return studentRepository.findById(id);
  }
}
