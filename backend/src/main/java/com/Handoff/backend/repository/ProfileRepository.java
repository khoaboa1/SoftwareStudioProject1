package com.Handoff.backend.repository;

import com.Handoff.backend.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Profile entities.
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

  /**
   * Checks if a profile already exists for a given student.
   *
   * @param studentId the ID of the student
   * @return true if a profile exists, false otherwise
   */
  boolean existsByStudent_Id(Long studentId);

  /**
   * Checks if a profile already exists for a given student.
   *
   * @param student the student entity
   * @return true if a profile exists, false otherwise
   */
  boolean existsByStudent(com.Handoff.backend.model.Student student);

  /**
   * Retrieves the profile associated with a given student.
   *
   * @param studentId the ID of the student
   * @return an Optional containing the Profile if found
   */
  Optional<Profile> findByStudent_Id(Long studentId);
}
