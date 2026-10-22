package com.springboot.EduSystem.assignment;

import java.time.Instant;

public record SubmissionDTO(
        Integer id,
        Integer assignmentId,
        String assignmentTitle,
        Integer studentId,
        String studentName,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant submittedAt,
        Integer grade,
        String feedback,
        Instant gradedAt
) {
}
