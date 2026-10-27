package com.Handoff.backend.repository;

import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProfileSearchQueryTest {

  @Autowired private ProfileRepository profileRepository;
  @Autowired private StudentRepository studentRepository;

  @Test
  void matchesPartialNamesCaseInsensitivelyInsideTheRequesterSchool() {
    Profile requester = profile("Alex Viewer", "search-viewer@tulane.edu", "tulane.edu");
    profile("Jane Doe", "jane@tulane.edu", "tulane.edu");
    profile("Janet Smith", "janet@tulane.edu", "tulane.edu");
    profile("Jane Loyola", "jane@loyola.edu", "loyola.edu");

    Page<Profile> result = search(requester, "JAN");

    assertThat(names(result)).containsExactly("Jane Doe", "Janet Smith");
  }

  @Test
  void excludesTheRequesterEvenWhenTheirNameMatches() {
    Profile requester = profile("Jane Viewer", "viewer@tulane.edu", "tulane.edu");
    profile("Jane Friend", "friend@tulane.edu", "tulane.edu");

    assertThat(names(search(requester, "jane"))).containsExactly("Jane Friend");
  }

  @Test
  void appliesStableSortingAndPagination() {
    Profile requester = profile("Viewer", "viewer@tulane.edu", "tulane.edu");
    profile("Ann Z", "z@tulane.edu", "tulane.edu");
    profile("Ann A", "a@tulane.edu", "tulane.edu");
    profile("Ann M", "m@tulane.edu", "tulane.edu");

    Sort sort = Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"));
    Page<Profile> result = profileRepository
        .findBySchoolDomainAndStudent_IdNotAndNameContainingIgnoreCase(
            requester.getSchoolDomain(), requester.getStudentId(), "ann", PageRequest.of(1, 2, sort));

    assertThat(names(result)).containsExactly("Ann Z");
    assertThat(result.getTotalElements()).isEqualTo(3);
    assertThat(result.getTotalPages()).isEqualTo(2);
  }

  private Page<Profile> search(Profile requester, String query) {
    Sort sort = Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"));
    return profileRepository.findBySchoolDomainAndStudent_IdNotAndNameContainingIgnoreCase(
        requester.getSchoolDomain(), requester.getStudentId(), query, PageRequest.of(0, 20, sort));
  }

  private List<String> names(Page<Profile> result) {
    return result.getContent().stream().map(Profile::getName).toList();
  }

  private Profile profile(String name, String email, String domain) {
    Student student = new Student(name, email, "hashed", null, null);
    student.setVerified(true);
    student = studentRepository.save(student);
    return profileRepository.save(new Profile(student, name, "Computer Science", "Bio", domain));
  }
}
