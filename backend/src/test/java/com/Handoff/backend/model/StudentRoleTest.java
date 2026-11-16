package com.Handoff.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

class StudentRoleTest {

    @Test
    void newStudentDefaultsToStudentRole() {
        assertThat(new Student().getRole()).isEqualTo(Role.STUDENT);
        assertThat(new Student("A", "a@tulane.edu", "h", null, null).getRole()).isEqualTo(Role.STUDENT);
        assertThat(new Student("A", "a@tulane.edu", "h", null, null, null).getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void setRoleChangesRole() {
        Student student = new Student();
        student.setRole(Role.ADMIN);
        assertThat(student.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void roleIsSerializedToJson() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        Student student = new Student();

        JsonNode tree = mapper.readTree(mapper.writeValueAsString(student));
        assertThat(tree.get("role").asString()).isEqualTo("STUDENT");

        student.setRole(Role.ADMIN);
        tree = mapper.readTree(mapper.writeValueAsString(student));
        assertThat(tree.get("role").asString()).isEqualTo("ADMIN");
    }
}
