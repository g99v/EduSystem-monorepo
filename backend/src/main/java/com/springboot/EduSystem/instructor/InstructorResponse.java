package com.springboot.EduSystem.instructor;

import com.springboot.EduSystem.user.Role;

public record InstructorResponse(
        Integer id,
        String firstName,
        String lastName,
        Integer userId,
        String email,
        Role role
) {
    public static InstructorResponse from(Instructor instructor) {
        return new InstructorResponse(instructor.getId(), instructor.getFirstName(),
                instructor.getLastName(), instructor.getUser().getId(),
                instructor.getUser().getEmail(), instructor.getUser().getRole());
    }
}
