package com.springboot.EduSystem.course;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CourseDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Integer id,

        @NotBlank(message = "Course name should not be blank.")
        @Size(max = 100, message = "Course name must not exceed 100 characters.")
        String name,

        @Size(max = 1000, message = "Course description must not exceed 1000 characters.")
        String description,

        @NotBlank(message = "Teacher name should not be blank.")
        @Size(max = 100, message = "Teacher name must not exceed 100 characters.")
        String teacherName,

        @Positive(message = "Instructor id must be positive.")
        Integer instructorId,

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String instructorName
) {
}
