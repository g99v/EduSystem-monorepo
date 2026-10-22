package com.springboot.EduSystem.enrollment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnrollmentCreateDTO(
        @NotNull(message = "Student id is required.")
        @Positive(message = "Student id must be positive.")
        Integer studentId,

        @NotNull(message = "Course id is required.")
        @Positive(message = "Course id must be positive.")
        Integer courseId
) {
}
