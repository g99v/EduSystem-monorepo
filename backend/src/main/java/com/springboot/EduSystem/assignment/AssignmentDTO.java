package com.springboot.EduSystem.assignment;

import java.time.Instant;

public record AssignmentDTO(
        Integer id,
        Integer courseId,
        String courseName,
        String title,
        String assignmentInstructions,
        int maxGrade,
        String originalFilename,
        String contentType,
        long fileSize,
        int displayOrder,
        Instant uploadedAt
) {
}
