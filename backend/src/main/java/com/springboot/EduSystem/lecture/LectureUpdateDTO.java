package com.springboot.EduSystem.lecture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LectureUpdateDTO(
        @NotBlank(message = "Lecture title should not be blank.")
        @Size(max = 200, message = "Lecture title must not exceed 200 characters.")
        String title,

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(message = "Display order must be zero or positive.")
        Integer displayOrder
) {
}
