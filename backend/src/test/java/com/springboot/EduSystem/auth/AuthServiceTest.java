package com.springboot.EduSystem.auth;

import com.springboot.EduSystem.school.SchoolRepository;
import com.springboot.EduSystem.student.Student;
import com.springboot.EduSystem.student.StudentRepository;
import com.springboot.EduSystem.user.Role;
import com.springboot.EduSystem.user.User;
import com.springboot.EduSystem.user.UserAccountService;
import com.springboot.EduSystem.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserAccountService accounts;
    @Mock UserRepository users;
    @Mock StudentRepository students;
    @Mock SchoolRepository schools;
    @Mock AuthenticationManager authenticationManager;
    @Mock Authentication authentication;
    @InjectMocks AuthService service;

    @Test
    void publicRegistrationAlwaysAssignsStudentRole() {
        User user = new User();
        when(accounts.create("student@example.com", "password123", Role.STUDENT)).thenReturn(user);
        when(students.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student student = service.registerStudent(new RegisterStudentRequest(
                "Jane", "Doe", "student@example.com", "password123", 21, null));

        assertSame(user, student.getUser());
        assertEquals(21, student.getAge());
        verify(accounts).create("student@example.com", "password123", Role.STUDENT);
    }

    @Test
    void verifyLoginReturnsProfileWithoutIssuingTokenOrSavingAuthentication() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 5);
        user.setEmail("student@example.com");
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("student@example.com");
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        SecurityContextHolder.clearContext();
        try {
            AuthResponse result = service.verifyLogin(new LoginRequest("student@example.com", "password123"));
            assertEquals(5, result.id());
            assertEquals(Role.STUDENT, result.role());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void invalidCredentialsDoNotIssueToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class,
                () -> service.verifyLogin(new LoginRequest("student@example.com", "wrong-password")));
        verifyNoInteractions(users);
    }
}
