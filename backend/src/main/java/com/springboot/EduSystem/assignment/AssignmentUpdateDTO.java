package com.springboot.EduSystem.assignment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AssignmentUpdateDTO(
        @NotBlank(message = "Assignment title should not be blank.")
        @Size(max = 200, message = "Assignment title must not exceed 200 characters.")
        String title,

        @Size(max = 10000, message = "Assignment instructions must not exceed 10000 characters.")
        String assignmentInstructions,

        @NotNull(message = "Maximum grade is required.")
        @Min(
                value = Assignment.MIN_MAX_GRADE,
                message = "Maximum grade must be at least " + Assignment.MIN_MAX_GRADE + "."
        )
        @Max(
                value = Assignment.MAX_MAX_GRADE,
                message = "Maximum grade must not exceed " + Assignment.MAX_MAX_GRADE + "."
        )
        Integer maxGrade,

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(message = "Display order must be zero or positive.")
        Integer displayOrder
) {
}
