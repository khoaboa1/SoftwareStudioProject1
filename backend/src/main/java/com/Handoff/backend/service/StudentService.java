package com.Handoff.backend.service;

import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.StudentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

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
        // The request body replaces the whole record; keep the stored role and suspension.
        studentRepository.findById(id).ifPresent(existing -> {
            updatedstudent.setRole(existing.getRole());
            updatedstudent.setSuspended(existing.isSuspended());
        });
        return studentRepository.save(updatedstudent);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }
}
