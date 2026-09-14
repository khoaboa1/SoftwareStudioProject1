package com.Handoff.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String studentName;

  @Column(unique = true, nullable = false)
  private String email;

  @JsonIgnore
  @Column(nullable = true)
  private String passwordHash;

  private String oauthProvider;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean emailVerified;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean verified;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @JsonIgnore
  private String verificationPin;

  @JsonIgnore
  private LocalDateTime verificationPinExpiry;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "student_trusted_devices", joinColumns = @JoinColumn(name = "student_id"))
  @Column(name = "device_id")
  private Set<String> trustedDeviceIds = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "student_selling_items", joinColumns = @JoinColumn(name = "student_id"))
  @Column(name = "item")
  private List<String> sellingItems;

  private String dormLocation;

  public Student() {
  }

  public Student(String studentName, String email, String passwordHash,
                 List<String> sellingItems, String dormLocation) {
    this(studentName, email, passwordHash, null, sellingItems, dormLocation);
  }

  public Student(String studentName, String email, String passwordHash,
                 String oauthProvider, List<String> sellingItems, String dormLocation) {
    this.studentName = studentName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.oauthProvider = oauthProvider;
    this.sellingItems = sellingItems != null ? new ArrayList<>(sellingItems) : null;
    this.dormLocation = dormLocation;
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public List<String> getSellingItems() {
    return sellingItems;
  }

  public void setSellingItems(List<String> sellingItems) {
    this.sellingItems = sellingItems;
  }

  public String getOauthProvider() {
    return oauthProvider;
  }

  public void setOauthProvider(String oauthProvider) {
    this.oauthProvider = oauthProvider;
  }

  public String getDormLocation() {
    return dormLocation;
  }

  public void setDormLocation(String dormLocation) {
    this.dormLocation = dormLocation;
  }

  public void addSellingItems(List<String> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    if (this.sellingItems == null) {
      this.sellingItems = new ArrayList<>();
    }
    this.sellingItems.addAll(items);
  }

  public boolean removeSellingItems(List<String> items) {
    if (this.sellingItems != null && items != null && !items.isEmpty()) {
      return this.sellingItems.removeAll(items);
    }
    return false;
  }

  public boolean updateSellingItems(List<String> oldItems, List<String> newItems) {
    boolean updated = false;
    if (this.sellingItems == null) {
      this.sellingItems = new ArrayList<>();
    }
    if (oldItems != null && !oldItems.isEmpty()) {
      updated = this.sellingItems.removeAll(oldItems);
    }
    if (newItems != null && !newItems.isEmpty()) {
      updated = this.sellingItems.addAll(newItems);
    }
    return updated;
  }

  public void updateSellingItems(List<String> newItems) {
    this.sellingItems = newItems != null ? new ArrayList<>(newItems) : null;
  }

  public void clearSellingItems() {
    if (this.sellingItems != null) {
      this.sellingItems.clear();
    }
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
    this.verified = emailVerified;
  }

  public boolean isVerified() {
    return verified;
  }

  public void setVerified(boolean verified) {
    this.verified = verified;
    this.emailVerified = verified;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  @jakarta.persistence.PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }

  public String getVerificationPin() {
    return verificationPin;
  }

  public void setVerificationPin(String verificationPin) {
    this.verificationPin = verificationPin;
  }

  public LocalDateTime getVerificationPinExpiry() {
    return verificationPinExpiry;
  }

  public void setVerificationPinExpiry(LocalDateTime verificationPinExpiry) {
    this.verificationPinExpiry = verificationPinExpiry;
  }

  public Set<String> getTrustedDeviceIds() {
    return trustedDeviceIds;
  }

  public void setTrustedDeviceIds(Set<String> trustedDeviceIds) {
    this.trustedDeviceIds = trustedDeviceIds != null ? trustedDeviceIds : new HashSet<>();
  }

  public boolean isDeviceTrusted(String deviceId) {
    if (deviceId == null || deviceId.isBlank()) {
      return false;
    }
    return this.trustedDeviceIds != null && this.trustedDeviceIds.contains(deviceId);
  }

  public void trustDevice(String deviceId) {
    if (deviceId != null && !deviceId.isBlank()) {
      if (this.trustedDeviceIds == null) {
        this.trustedDeviceIds = new HashSet<>();
      }
      this.trustedDeviceIds.add(deviceId);
    }
  }
}
