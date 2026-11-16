package com.Handoff.backend.controller;

import com.Handoff.backend.dto.AdminReportItem;
import com.Handoff.backend.dto.AdminStudentItem;
import com.Handoff.backend.dto.AdminStudentPageResponse;
import com.Handoff.backend.dto.ErrorResponse;
import com.Handoff.backend.dto.UpdateRoleRequest;
import com.Handoff.backend.service.AdminConflictException;
import com.Handoff.backend.service.AdminService;
import com.Handoff.backend.service.InvalidAdminActionException;
import com.Handoff.backend.service.ReportNotFoundException;
import com.Handoff.backend.service.StudentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminService service;
  private final CurrentStudentResolver resolver;
  public AdminController(AdminService service, CurrentStudentResolver resolver) {
    this.service = service;
    this.resolver = resolver;
  }
  @GetMapping("/reports")
  public List<AdminReportItem> reports(HttpServletRequest request) {
    return service.openReports(resolver.requireAdmin(request));
  }
  @PostMapping("/reports/{id}/dismiss")
  public ResponseEntity<Void> dismiss(@PathVariable Long id, HttpServletRequest request) {
    service.dismissReport(id, resolver.requireAdmin(request));
    return ResponseEntity.noContent().build();
  }
  @GetMapping("/students")
  public AdminStudentPageResponse students(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String limit,
                                           HttpServletRequest request) {
    return service.students(resolver.requireAdmin(request), q, page, limit);
  }
  @PostMapping("/students/{id}/suspend")
  public AdminStudentItem suspend(@PathVariable Long id, HttpServletRequest request) {
    return service.suspend(id, resolver.requireAdmin(request));
  }
  @PostMapping("/students/{id}/unsuspend")
  public AdminStudentItem unsuspend(@PathVariable Long id, HttpServletRequest request) {
    return service.unsuspend(id, resolver.requireAdmin(request));
  }
  @PutMapping("/students/{id}/role")
  public AdminStudentItem role(@PathVariable Long id, @RequestBody UpdateRoleRequest body,
                               HttpServletRequest request) {
    return service.changeRole(id, body.role(), resolver.requireAdmin(request));
  }
  @ExceptionHandler({ReportNotFoundException.class, StudentNotFoundException.class})
  public ResponseEntity<ErrorResponse> missing(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
  }
  @ExceptionHandler(InvalidAdminActionException.class)
  public ResponseEntity<ErrorResponse> invalid(InvalidAdminActionException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
  }
  @ExceptionHandler(AdminConflictException.class)
  public ResponseEntity<ErrorResponse> conflict(AdminConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
  }
}
