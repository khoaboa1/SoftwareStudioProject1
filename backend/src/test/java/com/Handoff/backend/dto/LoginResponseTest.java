package com.Handoff.backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.Handoff.backend.model.Role;
import com.Handoff.backend.model.Student;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

class LoginResponseTest {

    @Test
    void successIncludesStudentRole() {
        Student student = new Student("Jane", "jane@tulane.edu", "h", null, null);
        LoginResponse response = LoginResponse.success(student);
        assertEquals(Role.STUDENT, response.getRole());

        student.setRole(Role.ADMIN);
        LoginResponse adminResponse = LoginResponse.success(student);
        assertEquals(Role.ADMIN, adminResponse.getRole());
    }

    @Test
    void setRoleChangesRole() {
        LoginResponse response = new LoginResponse();
        assertNull(response.getRole());
        response.setRole(Role.ADMIN);
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    void requiresPinResponseHasNoRole() {
        LoginResponse response = LoginResponse.requiresPin("jane@tulane.edu", "msg");
        assertNull(response.getRole());
    }

    @Test
    void roleIsSerializedToJson() throws Exception {
        Student student = new Student("Jane", "jane@tulane.edu", "h", null, null);
        LoginResponse response = LoginResponse.success(student);

        JsonMapper mapper = JsonMapper.builder().build();
        String json = mapper.writeValueAsString(response);
        assertEquals("STUDENT", mapper.readTree(json).get("role").asString());
    }
}
