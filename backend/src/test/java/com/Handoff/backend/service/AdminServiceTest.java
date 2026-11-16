package com.Handoff.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingReportRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock
    ProfileRepository profiles;
    @Mock
    StudentRepository students;
    @Mock
    ListingReportRepository reports;
    private AdminService service;
    private Student admin;
    private Student target;

    @BeforeEach
    void setUp() {
        service = new AdminService(profiles, students, reports);
        admin = new Student("Admin", "admin@tulane.edu", "hash", null, null);
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        target = new Student("Target", "target@tulane.edu", "hash", null, null);
        target.setId(2L);
    }

    private void school() {
        when(profiles.findByStudent_Id(1L))
                .thenReturn(Optional.of(new Profile(admin, "Admin", "Administration", null, "tulane.edu")));
    }

    private void sameSchoolTarget() {
        school();
        when(profiles.findByStudentIdAndSchoolDomain(2L, "tulane.edu"))
                .thenReturn(Optional.of(new Profile(target, "Target", "CS", null, "tulane.edu")));
    }

    @Test
    void suspend_student_changesFlag() {
        sameSchoolTarget();
        assertThat(service.suspend(2L, admin).suspended()).isTrue();
    }

    @Test
    void suspend_admin_conflicts() {
        target.setRole(Role.ADMIN);
        sameSchoolTarget();
        assertThatThrownBy(() -> service.suspend(2L, admin)).isInstanceOf(AdminConflictException.class)
                .hasMessage("Remove admin role before suspending this student");
    }

    @Test
    void suspend_otherSchool_isNotFound() {
        school();
        assertThatThrownBy(() -> service.suspend(2L, admin)).isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void role_promotesAndDemotes() {
        sameSchoolTarget();
        assertThat(service.changeRole(2L, "ADMIN", admin).role()).isEqualTo("ADMIN");
        assertThat(service.changeRole(2L, "STUDENT", admin).role()).isEqualTo("STUDENT");
    }

    @Test
    void role_badValue_rejected() {
        sameSchoolTarget();
        assertThatThrownBy(() -> service.changeRole(2L, "OWNER", admin)).isInstanceOf(InvalidAdminActionException.class)
                .hasMessage("role must be ADMIN or STUDENT");
    }

    @Test
    void missingAdminProfile_404() {
        assertThatThrownBy(() -> service.openReports(admin)).isInstanceOf(ProfileNotFoundException.class)
                .hasMessage("Profile not found");
    }
}
