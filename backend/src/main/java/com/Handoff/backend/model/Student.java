package com.Handoff.backend.model;

import java.util.ArrayList;
import java.util.List;

public class Student {
  private Long id;
  private String studentName;
  private String email;
  private List<String> sellingItems;
  private String dormLocation;

  // Default Constructor
  public Student() {
  }

  // Create student account with (optional) selling list/dorm location (since Id,
  // name,
  // and email are required fields)

  public Student(Long id, String studentName, String email, List<String> sellingItems, String dormLocation) {
    this.id = id;
    this.studentName = studentName;
    this.email = email;
    this.sellingItems = sellingItems != null ? new ArrayList<>(sellingItems) : null;
    this.dormLocation = dormLocation != null ? dormLocation : null;
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

  // Helper methods to update selling items

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
