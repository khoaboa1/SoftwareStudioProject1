package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

  private final StudentRepository studentRepository;

  public StudentService(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
  }

  public List<Student> getAllStudents() {
    return studentRepository.findAll();
  }

  public Student createStudent(Student student) {
    return studentRepository.save(student);
  }

  public Student updateStudent(Long id, Student updatedstudent) {
    updatedstudent.setId(id);
    return studentRepository.save(updatedstudent);
  }

  public void deleteStudent(Long id) {
    studentRepository.deleteById(id);
  }
}
