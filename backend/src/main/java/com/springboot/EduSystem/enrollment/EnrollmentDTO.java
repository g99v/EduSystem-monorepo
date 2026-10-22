package com.springboot.EduSystem.enrollment;

import java.time.Instant;

public record EnrollmentDTO(
        Integer id,
        Integer studentId,
        String studentName,
        Integer courseId,
        String courseName,
        Instant enrolledAt,
        EnrollmentStatus status,
        String grade
) {
}
