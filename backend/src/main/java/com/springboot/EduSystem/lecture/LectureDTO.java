package com.springboot.EduSystem.lecture;

import java.time.Instant;

public record LectureDTO(
        Integer id,
        Integer courseId,
        String courseName,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        int displayOrder,
        Instant uploadedAt
) {
}
