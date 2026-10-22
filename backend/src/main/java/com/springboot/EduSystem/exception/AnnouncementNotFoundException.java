package com.springboot.EduSystem.exception;

public class AnnouncementNotFoundException extends RuntimeException {
    public AnnouncementNotFoundException(Integer announcementId) {
        super("Announcement with id " + announcementId + " was not found");
    }
}
