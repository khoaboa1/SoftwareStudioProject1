package com.Handoff.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

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
  @Column(nullable = false)
  private String passwordHash;

  @ElementCollection
  @CollectionTable(name = "student_selling_items", joinColumns = @JoinColumn(name = "student_id"))
  @Column(name = "item")
  private List<String> sellingItems;

  private String dormLocation;

  public Student() {
  }

  public Student(String studentName, String email, String passwordHash,
                 List<String> sellingItems, String dormLocation) {
    this.studentName = studentName;
    this.email = email;
    this.passwordHash = passwordHash;
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
}
