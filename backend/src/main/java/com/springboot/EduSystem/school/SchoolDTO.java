package com.springboot.EduSystem.school;

import jakarta.validation.constraints.NotEmpty;

public record SchoolDTO(
        @NotEmpty(message = "School name should not be empty.")
        String name
) {
}
