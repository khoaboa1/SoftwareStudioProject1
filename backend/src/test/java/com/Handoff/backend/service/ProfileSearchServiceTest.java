package com.Handoff.backend.service;

import com.Handoff.backend.dto.ProfileSearchPageResponse;
import com.Handoff.backend.dto.ProfileSearchRequest;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProfileSearchServiceTest {

  @Autowired private ProfileService profileService;
  @Autowired private ProfileRepository profileRepository;
  @Autowired private StudentRepository studentRepository;

  @Test
  void mapsCompactItemsAndPaginationMetadata() {
    Student requester = profile("Viewer", "service-viewer@tulane.edu", "tulane.edu").getStudent();
    profile("Jane Alpha", "service-alpha@tulane.edu", "tulane.edu");
    profile("Jane Beta", "service-beta@tulane.edu", "tulane.edu");
    profile("Jane Gamma", "service-gamma@tulane.edu", "tulane.edu");

    ProfileSearchPageResponse response = profileService.searchProfiles(
        requester, ProfileSearchRequest.parse("jane", "2", "2"));

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().name()).isEqualTo("Jane Gamma");
    assertThat(response.items().getFirst().studentId()).isNotNull();
    assertThat(response.items().getFirst().major()).isEqualTo("Computer Science");
    assertThat(response.items().getFirst().bio()).isEqualTo("Bio");
    assertThat(response.meta().totalItems()).isEqualTo(3);
    assertThat(response.meta().currentPage()).isEqualTo(2);
    assertThat(response.meta().totalPages()).isEqualTo(2);
    assertThat(response.meta().pageSize()).isEqualTo(2);
  }

  @Test
  void returnsAnEmptySuccessfulPageWhenNothingMatches() {
    Student requester = profile("Viewer", "service-empty@tulane.edu", "tulane.edu").getStudent();

    ProfileSearchPageResponse response = profileService.searchProfiles(
        requester, ProfileSearchRequest.parse("nobody", null, null));

    assertThat(response.items()).isEmpty();
    assertThat(response.meta().totalItems()).isZero();
    assertThat(response.meta().currentPage()).isEqualTo(1);
  }

  @Test
  void reportsWhenTheAuthenticatedStudentHasNoProfile() {
    Student requester = student("Profileless", "service-none@tulane.edu");

    assertThatThrownBy(() -> profileService.searchProfiles(
        requester, ProfileSearchRequest.parse("jane", null, null)))
        .isInstanceOf(ProfileNotFoundException.class)
        .hasMessage("Profile not found");
  }

  private Profile profile(String name, String email, String domain) {
    Student student = student(name, email);
    return profileRepository.save(new Profile(student, name, "Computer Science", "Bio", domain));
  }

  private Student student(String name, String email) {
    Student student = new Student(name, email, "hashed", null, null);
    student.setVerified(true);
    return studentRepository.save(student);
  }
}
