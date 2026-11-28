package com.Handoff.backend.service;

import com.Handoff.backend.dto.PublicProfileResponse;
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
class PublicProfileServiceTest {
    @Autowired
    private ProfileService profileService;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private StudentRepository studentRepository;

    @Test
    void rejectsAMissingRequester() {
        assertThatThrownBy(() -> profileService.getPublicProfile(null, 1L))
                .isInstanceOf(NotAuthenticatedException.class)
                .hasMessage("User must be authenticated to view profiles.");
    }

    @Test
    void mapsTheSameSchoolProfileWithoutTheEmail() {
        Student requester = profile("Viewer", "public-viewer@tulane.edu", "tulane.edu").getStudent();
        Profile target = profile("Jane Doe", "public-jane@tulane.edu", "tulane.edu");
        PublicProfileResponse response = profileService.getPublicProfile(requester, target.getStudentId());
        assertThat(response.studentId()).isEqualTo(target.getStudentId());
        assertThat(response.name()).isEqualTo("Jane Doe");
        assertThat(response.schoolDomain()).isEqualTo("tulane.edu");
        assertThat(response.verified()).isTrue();
    }

    @Test
    void hidesAStudentFromAnotherSchool() {
        Student requester = profile("Viewer", "public-viewer2@tulane.edu", "tulane.edu").getStudent();
        Profile target = profile("Lee Loyola", "public-lee@loyola.edu", "loyola.edu");
        assertThatThrownBy(() -> profileService.getPublicProfile(requester, target.getStudentId()))
                .isInstanceOf(ProfileNotFoundException.class).hasMessage("Profile not found");
    }

    private Profile profile(String name, String email, String domain) {
        Student student = new Student(name, email, "hashed", null, null);
        student.setVerified(true);
        student = studentRepository.save(student);
        return profileRepository.save(new Profile(student, name, "Computer Science", "Bio", domain));
    }
}
