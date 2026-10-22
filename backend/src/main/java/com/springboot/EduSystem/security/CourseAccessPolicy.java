package com.springboot.EduSystem.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.enrollment.EnrollmentRepository;
import com.springboot.EduSystem.instructor.Instructor;
import com.springboot.EduSystem.instructor.InstructorRepository;
import com.springboot.EduSystem.student.Student;
import com.springboot.EduSystem.student.StudentRepository;

@Component
public class CourseAccessPolicy {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_INSTRUCTOR = "ROLE_INSTRUCTOR";
    public static final String ROLE_STUDENT = "ROLE_STUDENT";

    private final EnrollmentRepository enrollments;
    private final InstructorRepository instructors;
    private final StudentRepository students;

    public CourseAccessPolicy(
            EnrollmentRepository enrollments,
            InstructorRepository instructors,
            StudentRepository students
    ) {
        this.enrollments = enrollments;
        this.instructors = instructors;
        this.students = students;
    }

    public boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN);
    }

    public boolean isInstructor(Authentication authentication) {
        return hasRole(authentication, ROLE_INSTRUCTOR);
    }


    public boolean isCourseManager(Course course, Authentication authentication) {
        if (isAdmin(authentication)) {
            return true;
        }
        if (!isInstructor(authentication)) {
            return false;
        }

        Instructor instructor = instructors
                .findByUser_EmailIgnoreCase(authentication.getName())
                .orElse(null);

        return instructor != null
                && course.getInstructor() != null
                && course.getInstructor().getId().equals(instructor.getId());
    }


    public void requireAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("You are not allowed to list records for every course");
        }
    }

    public void requireCourseManagement(Course course, Authentication authentication) {
        if (isCourseManager(course, authentication)) {
            return;
        }

        throw new AccessDeniedException("You do not manage this course");
    }

    public void requireCourseReadAccess(Course course, Authentication authentication) {
        if (isAdmin(authentication)) {
            return;
        }

        if (isInstructor(authentication)) {
            requireCourseManagement(course, authentication);
            return;
        }

        requireEnrollment(course, requireStudent(authentication));
    }

    public Instructor requireInstructor(Authentication authentication) {
        return instructors.findByUser_EmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "No instructor profile for this account"));
    }

    public Student requireStudent(Authentication authentication) {
        return students.findByUser_EmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "No student profile for this account"));
    }

    public Student requireEnrolledStudent(Course course, Authentication authentication) {
        Student student = requireStudent(authentication);
        requireEnrollment(course, student);
        return student;
    }

    private void requireEnrollment(Course course, Student student) {
        if (!enrollments.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new AccessDeniedException("You are not enrolled in this course");
        }
    }
}
