package com.Handoff.backend.dto;

import com.Handoff.backend.model.Student;

import java.util.List;

public class LoginResponse {
  private boolean requiresPin;
  private Long id;
  private String studentName;
  private String email;
  private List<String> sellingItems;
  private String dormLocation;
  private boolean emailVerified;
  private String message;

  public LoginResponse() {
  }

  public static LoginResponse success(Student student) {
    LoginResponse res = new LoginResponse();
    res.requiresPin = false;
    res.id = student.getId();
    res.studentName = student.getStudentName();
    res.email = student.getEmail();
    res.sellingItems = student.getSellingItems();
    res.dormLocation = student.getDormLocation();
    res.emailVerified = student.isEmailVerified();
    res.message = "Login successful";
    return res;
  }

  public static LoginResponse requiresPin(String email, String message) {
    LoginResponse res = new LoginResponse();
    res.requiresPin = true;
    res.email = email;
    res.message = message;
    return res;
  }

  public boolean isRequiresPin() {
    return requiresPin;
  }

  public void setRequiresPin(boolean requiresPin) {
    this.requiresPin = requiresPin;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getStudentName() {
    return studentName;
  }

  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<String> getSellingItems() {
    return sellingItems;
  }

  public void setSellingItems(List<String> sellingItems) {
    this.sellingItems = sellingItems;
  }

  public String getDormLocation() {
    return dormLocation;
  }

  public void setDormLocation(String dormLocation) {
    this.dormLocation = dormLocation;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
