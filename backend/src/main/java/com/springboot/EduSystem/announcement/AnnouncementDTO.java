package com.springboot.EduSystem.announcement;

import java.time.Instant;

public record AnnouncementDTO(
        Integer id,
        Integer courseId,
        String courseName,
        String title,
        String body,
        Instant createdAt
) {
}
