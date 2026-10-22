package com.springboot.EduSystem.announcement;

import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {
    public AnnouncementDTO toAnnouncementDTO(Announcement announcement) {
        Objects.requireNonNull(announcement, "Announcement should not be null");

        return new AnnouncementDTO(
                announcement.getId(),
                announcement.getCourse().getId(),
                announcement.getCourse().getName(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getCreatedAt()
        );
    }
}
