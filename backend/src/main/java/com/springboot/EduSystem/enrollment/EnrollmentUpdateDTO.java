package com.springboot.EduSystem.enrollment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentUpdateDTO(
        @NotNull(message = "Enrollment status is required.")
        EnrollmentStatus status,

        @Size(max = 10, message = "Grade must not exceed 10 characters.")
        String grade
) {
}
