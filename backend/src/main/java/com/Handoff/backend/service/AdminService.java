package com.Handoff.backend.service;

import com.Handoff.backend.dto.AdminReportItem;
import com.Handoff.backend.dto.AdminStudentItem;
import com.Handoff.backend.dto.AdminStudentPageResponse;
import com.Handoff.backend.model.Profile;
import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Student;
import com.Handoff.backend.repository.ListingReportRepository;
import com.Handoff.backend.repository.ProfileRepository;
import com.Handoff.backend.repository.StudentRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final ProfileRepository profiles;
  private final StudentRepository students;
  private final ListingReportRepository reports;
  public AdminService(ProfileRepository profiles, StudentRepository students, ListingReportRepository reports) {
    this.profiles = profiles;
    this.students = students;
    this.reports = reports;
  }
  private String domain(Student admin) {
    return profiles.findByStudent_Id(admin.getId())
        .orElseThrow(() -> new ProfileNotFoundException("Profile not found")).getSchoolDomain();
  }
  private Profile target(Long id, String domain) {
    return profiles.findByStudentIdAndSchoolDomain(id, domain).orElseThrow(StudentNotFoundException::new);
  }
  @Transactional(readOnly = true)
  public List<AdminReportItem> openReports(Student admin) {
    return reports.findOpenForSchool(domain(admin)).stream().map(AdminReportItem::from).toList();
  }
  @Transactional
  public void dismissReport(Long id, Student admin) {
    var report = reports.findByIdForSchool(id, domain(admin)).orElseThrow(ReportNotFoundException::new);
    report.dismiss();
    reports.save(report);
  }
  @Transactional(readOnly = true)
  public AdminStudentPageResponse students(Student admin, String q, String page, String limit) {
    String school = domain(admin);
    String query = q == null || q.isBlank() ? null : q.trim();
    if (query != null && query.length() > 100) throw new InvalidAdminActionException("q cannot exceed 100 characters");
    int pageNumber = number("page", page, 1);
    int pageSize = number("limit", limit, 20);
    if (pageNumber < 1) throw new InvalidAdminActionException("page must be at least 1");
    if (pageSize < 1 || pageSize > 100) throw new InvalidAdminActionException("limit must be between 1 and 100");
    var result = profiles.findAdminStudents(school, query,
        PageRequest.of(pageNumber - 1, pageSize, Sort.by("name").ascending().and(Sort.by("id"))));
    return new AdminStudentPageResponse(result.getContent().stream().map(AdminStudentItem::from).toList(),
        new AdminStudentPageResponse.Meta(result.getTotalElements(), pageNumber, result.getTotalPages(), pageSize));
  }
  private int number(String name, String value, int fallback) {
    if (value == null || value.isBlank()) return fallback;
    try { return Integer.parseInt(value.trim()); }
    catch (NumberFormatException ex) { throw new InvalidAdminActionException(name + " must be a valid integer"); }
  }
  @Transactional
  public AdminStudentItem suspend(Long id, Student admin) {
    Profile profile = target(id, domain(admin));
    Student student = profile.getStudent();
    if (student.getId().equals(admin.getId())) throw new InvalidAdminActionException("You cannot suspend yourself");
    if (student.getRole() == Role.ADMIN) throw new AdminConflictException("Remove admin role before suspending this student");
    student.setSuspended(true);
    students.save(student);
    return AdminStudentItem.from(profile);
  }
  @Transactional
  public AdminStudentItem unsuspend(Long id, Student admin) {
    Profile profile = target(id, domain(admin));
    profile.getStudent().setSuspended(false);
    students.save(profile.getStudent());
    return AdminStudentItem.from(profile);
  }
  @Transactional
  public AdminStudentItem changeRole(Long id, String role, Student admin) {
    Profile profile = target(id, domain(admin));
    if (id.equals(admin.getId())) throw new InvalidAdminActionException("You cannot change your own role");
    if (!"ADMIN".equals(role) && !"STUDENT".equals(role)) {
      throw new InvalidAdminActionException("role must be ADMIN or STUDENT");
    }
    profile.getStudent().setRole(Role.valueOf(role));
    students.save(profile.getStudent());
    return AdminStudentItem.from(profile);
  }
}
