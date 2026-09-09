package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class StudentService {

  public List<Student> getAllStudents() {
    return Arrays.asList(
        new Student(1L, "Sarah", "sarah@tulane.edu", Arrays.asList("Desk Lamp", "Mini Fridge"), "Wall Residence Hall"),
        new Student(2L, "Alex", "alex@tulane.edu", Arrays.asList("Study Desk", "Office Chair"), "Aron Residences"),
        new Student(3L, "Chloe", "chloe@tulane.edu", Arrays.asList("Bedside Fan", "Storage Bins"), "Weatherhead"),
        new Student(4L, "Brian", "brian@tulane.edu", null, null));
  }
}
