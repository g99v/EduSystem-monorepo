package com.springboot.EduSystem.exception;

public class LectureNotFoundException extends RuntimeException {
    public LectureNotFoundException(Integer lectureId) {
        super("Lecture with id " + lectureId + " was not found");
    }
}
