package com.springboot.EduSystem.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record StudentDTO(
        @NotEmpty(message = "Firstname should not be empty.") @Size(max = 20)
        String firstName,
        @NotEmpty(message = "Lastname should not be empty.") @Size(max = 255)
        String lastName,
        @NotBlank(message = "Email should not be blank.")
        @Email(message = "Email must be valid.") @Size(max = 255)
        String email,
        @NotBlank @Size(min = 8)
        String password,
        @NotNull @PositiveOrZero
        Integer age,
        @NotNull(message = "School id is required.")
        @Positive(message = "School id must be positive.")
        Integer schoolId
) {
}
