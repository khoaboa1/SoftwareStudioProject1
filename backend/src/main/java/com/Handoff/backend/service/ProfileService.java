package com.Handoff.backend.service;

import com.Handoff.backend.dto.ProfileSearchItem;
import com.Handoff.backend.dto.ProfileSearchPageResponse;
import com.Handoff.backend.dto.ProfileSearchRequest;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

  /**
   * Retrieves a specific profile by its ID, enforcing authorization.
   *
   * @param id the profile ID
   * @param student authenticated student entity
   * @return the Profile
   * @throws ProfileNotFoundException if profile does not exist
   * @throws ProfileAccessDeniedException if the profile does not belong to the student
   */
  @Transactional(readOnly = true)
  public Profile getProfileById(Long id, Student student) {
    if (student == null) {
      throw new NotAuthenticatedException("Student must be authenticated to retrieve a profile.");
    }
    Profile profile = profileRepository.findById(id)
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

    if (!profile.getStudent().getId().equals(student.getId())) {
      throw new ProfileAccessDeniedException("You do not have permission to access this profile.");
    }

    return profile;
  }

  /**
   * Updates a specific profile by its ID, enforcing authorization.
   *
   * @param id the profile ID
   * @param student authenticated student entity
   * @param name new name (optional)
   * @param major new major (optional)
   * @param bio new bio (optional)
   * @return the updated Profile
   */
  @Transactional
  public Profile updateProfile(Long id, Student student, String name, String major, String bio) {
    if (student == null) {
      throw new NotAuthenticatedException("Student must be authenticated to update a profile.");
    }
    Profile profile = profileRepository.findById(id)
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

    if (!profile.getStudent().getId().equals(student.getId())) {
      throw new ProfileAccessDeniedException("You do not have permission to modify this profile.");
    }

    if (name != null && !name.isBlank()) {
      profile.setName(name.trim());
    }
    if (major != null && !major.isBlank()) {
      profile.setMajor(major.trim());
    }
    if (bio != null) {
      profile.setBio(bio.trim());
    }

    return profileRepository.save(profile);
  }

  /**
   * Searches public profile cards within the requester's school tenant.
   * The service owns tenant resolution, persistence access, and response mapping;
   * callers cannot supply or override a school domain.
   */
  @Transactional(readOnly = true)
  public ProfileSearchPageResponse searchProfiles(Student requester, ProfileSearchRequest request) {
    if (requester == null) {
      throw new NotAuthenticatedException("User must be authenticated to search profiles.");
    }

    Profile requesterProfile = profileRepository.findByStudent_Id(requester.getId())
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

    Sort sort = Sort.by(Sort.Direction.ASC, "name")
        .and(Sort.by(Sort.Direction.ASC, "id"));
    PageRequest pageRequest = PageRequest.of(
        request.page() - 1, request.limit(), sort);
    Page<Profile> result = profileRepository
        .findBySchoolDomainAndStudent_IdNotAndNameContainingIgnoreCase(
            requesterProfile.getSchoolDomain(), requester.getId(),
            request.query(), pageRequest);

    List<ProfileSearchItem> items = result.getContent().stream()
        .map(ProfileSearchItem::from)
        .toList();
    return new ProfileSearchPageResponse(items, new ProfileSearchPageResponse.Meta(
        result.getTotalElements(), request.page(), result.getTotalPages(), request.limit()));
  }

}
