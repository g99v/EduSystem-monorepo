package com.springboot.EduSystem.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.springboot.EduSystem.instructor.Instructor;
import com.springboot.EduSystem.instructor.InstructorRepository;
import com.springboot.EduSystem.school.School;
import com.springboot.EduSystem.school.SchoolRepository;
import com.springboot.EduSystem.student.Student;
import com.springboot.EduSystem.student.StudentRepository;
import com.springboot.EduSystem.user.Role;
import com.springboot.EduSystem.user.User;
import com.springboot.EduSystem.user.UserAccountService;
import com.springboot.EduSystem.user.UserRepository;

@Service
public class AuthService {
    private final UserAccountService accounts;
    private final UserRepository users;
    private final StudentRepository students;
    private final SchoolRepository schools;
    private final InstructorRepository instructors;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserAccountService accounts, UserRepository users,
                       StudentRepository students, SchoolRepository schools,
                       InstructorRepository instructors,
                       AuthenticationManager authenticationManager) {
        this.accounts = accounts;
        this.users = users;
        this.students = students;
        this.schools = schools;
        this.instructors = instructors;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public Student registerStudent(RegisterStudentRequest request) {
        School school = null;
        if (request.schoolId() != null) {
            school = schools.findById(request.schoolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
        }
        User user = accounts.create(request.email(), request.password(), Role.STUDENT);
        Student student = new Student(request.firstName(), request.lastName(), request.age(), user);
        student.setSchool(school);
        return students.save(student);
    }

    @Transactional(readOnly = true)
    public AuthResponse verifyLogin(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        User user = findAuthenticatedUser(authentication);

        return AuthResponse.from(user, findStudentId(user), findInstructorId(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse currentUser(Authentication authentication) {
        User user = findAuthenticatedUser(authentication);
        return AuthResponse.from(user, findStudentId(user), findInstructorId(user));
    }

    private User findAuthenticatedUser(Authentication authentication) {
        User user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Authenticated account not found"));
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }
        return user;
    }

    private Integer findStudentId(User user) {
        if (user.getRole() != Role.STUDENT) {
            return null;
        }

        return students.findByUser_EmailIgnoreCase(user.getEmail())
                .map(Student::getId)
                .orElse(null);
    }

    private Integer findInstructorId(User user) {
        if (user.getRole() != Role.INSTRUCTOR) {
            return null;
        }

        return instructors.findByUser_EmailIgnoreCase(user.getEmail())
                .map(Instructor::getId)
                .orElse(null);
    }
}
