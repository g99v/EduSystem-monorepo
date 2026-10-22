package com.springboot.EduSystem.student;

import com.springboot.EduSystem.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentMapperTest {
    @Test
    void responseUsesAccountEmail() {
        User user = new User();
        user.setEmail("student@example.com");
        Student student = new Student("John", "Doe", 22, user);

        StudentResponseDTO response = new StudentMapper().toStudentResponseDTO(student);

        assertEquals("student@example.com", response.email());
        assertEquals("John", response.firstName());
    }
}
