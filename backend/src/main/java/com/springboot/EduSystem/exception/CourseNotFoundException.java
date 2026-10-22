package com.springboot.EduSystem.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Integer courseId) {
        super("Course with id " + courseId + " was not found");
    }
}
