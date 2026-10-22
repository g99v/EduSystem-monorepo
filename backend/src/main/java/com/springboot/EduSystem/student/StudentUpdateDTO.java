package com.springboot.EduSystem.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StudentUpdateDTO(
        @Pattern(regexp = ".*\\S.*", message = "Firstname should not be blank.") @Size(max = 20)
        String firstName,
        @Pattern(regexp = ".*\\S.*", message = "Lastname should not be blank.") @Size(max = 255)
        String lastName,
        @Pattern(regexp = ".*\\S.*", message = "Email should not be blank.")
        @Email(message = "Email must be valid.") @Size(max = 255)
        String email,
        @Positive(message = "School id must be positive.")
        Integer schoolId
) {
}
