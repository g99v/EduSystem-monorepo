package com.springboot.EduSystem.student;

import com.springboot.EduSystem.auth.AuthService;
import com.springboot.EduSystem.auth.RegisterStudentRequest;
import com.springboot.EduSystem.exception.EmailAlreadyExistsException;
import com.springboot.EduSystem.school.SchoolRepository;
import com.springboot.EduSystem.user.User;
import com.springboot.EduSystem.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock StudentRepository students;
    @Mock SchoolRepository schools;
    @Mock StudentMapper mapper;
    @Mock UserRepository users;
    @Mock AuthService authService;
    @InjectMocks StudentService service;

    @Test
    void legacyCreateUsesRegistrationService() {
        StudentDTO request = new StudentDTO("John", "Doe", "john@example.com", "password123", 21, 1);
        User user = new User();
        user.setEmail("john@example.com");
        Student student = new Student("John", "Doe", 21, user);
        StudentResponseDTO response = new StudentResponseDTO("John", "Doe", "john@example.com");
        when(schools.existsById(1)).thenReturn(true);
        when(authService.registerStudent(any(RegisterStudentRequest.class))).thenReturn(student);
        when(mapper.toStudentResponseDTO(student)).thenReturn(response);

        var result = service.saveStudent(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(authService).registerStudent(new RegisterStudentRequest(
                "John", "Doe", "john@example.com", "password123", 21, 1));
    }

    @Test
    void updateEmailChecksAccountUniqueness() {
        User user = new User();
        user.setEmail("john@example.com");
        Student student = new Student("John", "Doe", 21, user);
        when(students.findById(1)).thenReturn(Optional.of(student));
        when(users.existsByEmailIgnoreCaseAndIdNot("taken@example.com", null)).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> service.updateStudentById(
                1, new StudentUpdateDTO(null, null, "taken@example.com", null)));
        assertEquals("john@example.com", user.getEmail());
    }
}
