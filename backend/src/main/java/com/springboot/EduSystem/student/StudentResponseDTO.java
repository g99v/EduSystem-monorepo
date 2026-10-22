package com.springboot.EduSystem.student;

public record StudentResponseDTO(
        String firstName,
        String lastName,
        String email
) {
}
