package com.springboot.EduSystem.enrollment;

import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.student.Student;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EnrollmentMapper {
    public Enrollment toEnrollment(Student student, Course course) {
        Objects.requireNonNull(student, "Student should not be null");
        Objects.requireNonNull(course, "Course should not be null");
        return new Enrollment(student, course);
    }

    public EnrollmentDTO toEnrollmentDTO(Enrollment enrollment) {
        Objects.requireNonNull(enrollment, "Enrollment should not be null");

        Student student = enrollment.getStudent();
        Course course = enrollment.getCourse();
        String studentName = (student.getFirstName() + " " + student.getLastName()).trim();

        return new EnrollmentDTO(
                enrollment.getId(),
                student.getId(),
                studentName,
                course.getId(),
                course.getName(),
                enrollment.getEnrolledAt(),
                enrollment.getStatus(),
                enrollment.getGrade()
        );
    }

    public void updateEnrollment(Enrollment enrollment, EnrollmentUpdateDTO dto) {
        Objects.requireNonNull(enrollment, "Enrollment should not be null");
        Objects.requireNonNull(dto, "EnrollmentUpdateDTO should not be null");

        enrollment.setStatus(dto.status());
        enrollment.setGrade(normalizeNullable(dto.grade()));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
