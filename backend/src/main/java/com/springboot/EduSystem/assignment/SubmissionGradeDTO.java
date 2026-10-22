package com.springboot.EduSystem.assignment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmissionGradeDTO(
        @NotNull(message = "Grade is required.")
        @Min(
                value = Assignment.MIN_MAX_GRADE,
                message = "Grade must be at least " + Assignment.MIN_MAX_GRADE + "."
        )
        @Max(
                value = Assignment.MAX_MAX_GRADE,
                message = "Grade must not exceed " + Assignment.MAX_MAX_GRADE + "."
        )
        Integer grade,

        @Size(max = 10000, message = "Feedback must not exceed 10000 characters.")
        String feedback
) {
}
