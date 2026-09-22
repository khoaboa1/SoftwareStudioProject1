package com.Handoff.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Entity representing a student's public and marketplace profile.
 * Maps to the 'profiles' database table.
 */
@Entity
@Table(name = "profiles")
public class Profile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * One-to-one relationship linking this profile to the authenticated student.
   * Marked with @JsonIgnore to prevent leaking student credentials or circular JSON serialization.
   * Cascades on delete so that removing a student removes their profile.
   */
  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false, unique = true)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Student student;

  /**
   * Student's display name on their profile.
   */
  @Column(nullable = false)
  private String name;

  /**
   * Academic major at university.
   */
  @Column(nullable = false)
  private String major;

  /**
   * Optional biography/description for the profile.
   */
  @Column(length = 1000)
  private String bio;

  /**
   * Extracted school domain (e.g. 'tulane.edu') parsed from user email.
   * Acts as tenant identifier for marketplace scoping.
   */
  @Column(name = "school_domain", nullable = false)
  private String schoolDomain;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public Profile() {
  }

  public Profile(Student student, String name, String major, String bio, String schoolDomain) {
    this.student = student;
    this.name = name;
    this.major = major;
    this.bio = bio;
    this.schoolDomain = schoolDomain;
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Student getStudent() {
    return student;
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  /**
   * Helper property to expose the student's ID in JSON responses without exposing full Student entity.
   */
  @JsonProperty("studentId")
  public Long getStudentId() {
    return student != null ? student.getId() : null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMajor() {
    return major;
  }

  public void setMajor(String major) {
    this.major = major;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getSchoolDomain() {
    return schoolDomain;
  }

  public void setSchoolDomain(String schoolDomain) {
    this.schoolDomain = schoolDomain;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
