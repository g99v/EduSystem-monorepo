package com.springboot.EduSystem.auth;

import com.springboot.EduSystem.user.Role;
import com.springboot.EduSystem.user.User;

public record AuthResponse(
        Integer id,
        String email,
        Role role,
        Integer studentId,
        Integer instructorId
) {
    public static AuthResponse from(User user, Integer studentId, Integer instructorId) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                studentId,
                instructorId
        );
    }
}
