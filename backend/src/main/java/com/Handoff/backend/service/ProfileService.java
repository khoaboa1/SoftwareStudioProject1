package com.Handoff.backend.service;

import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing student profile creation, retrieval, and domain extraction.
 */
@Service
public class ProfileService {

  private final ProfileRepository profileRepository;

  public ProfileService(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  /**
   * Creates a student profile for an authenticated user.
   *
   * @param student authenticated student entity
   * @param name    display name of the student
   * @param major   academic major
   * @param bio     optional biography
   * @return newly created and persisted Profile
   * @throws DuplicateProfileException if the user already possesses a profile
   */
  @Transactional
  public Profile createProfile(Student student, String name, String major, String bio) {
    // 1. Verify authentication presence
    if (student == null) {
      throw new NotAuthenticatedException("Student must be authenticated to create a profile.");
    }

    // 2. Prevent duplicate profile creation for the same account
    if (profileRepository.existsByStudent_Id(student.getId())) {
      throw new DuplicateProfileException("A profile already exists for this account.");
    }

    // =========================================================================
    // START: DOMAIN EXTRACTION LOGIC
    // -------------------------------------------------------------------------
    // Extracts school domain (e.g., 'tulane.edu') from student's authenticated email.
    // This domain acts as the tenant identifier for future marketplace scoping.
    // If no '@' is present, defaults to an empty string "" per requirements.
    // MODIFY THIS BLOCK IF DOMAIN PARSING RULES CHANGE IN THE FUTURE:
    // =========================================================================
    String email = student.getEmail();
    String schoolDomain = "";
    if (email != null && email.contains("@")) {
      int atIndex = email.indexOf("@");
      if (atIndex + 1 < email.length()) {
        schoolDomain = email.substring(atIndex + 1).toLowerCase().trim();
      }
    }
    // =========================================================================
    // END: DOMAIN EXTRACTION LOGIC
    // =========================================================================

    // 3. Build and save the profile
    Profile profile = new Profile(
        student,
        name != null ? name.trim() : null,
        major != null ? major.trim() : null,
        bio != null ? bio.trim() : null,
        schoolDomain
    );

    return profileRepository.save(profile);
  }

  /**
   * Retrieves the authenticated student's profile.
   *
   * @param student authenticated student entity
   * @return the student's Profile
   * @throws ProfileNotFoundException if no profile exists for the student
   */
  @Transactional(readOnly = true)
  public Profile getMyProfile(Student student) {
    if (student == null) {
      throw new NotAuthenticatedException("Student must be authenticated to retrieve a profile.");
    }
    return profileRepository.findByStudent_Id(student.getId())
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));
  }
}
