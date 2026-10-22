package com.springboot.EduSystem.exception;

public class InstructorNotFoundException extends RuntimeException {
    public InstructorNotFoundException(Integer instructorId) {
        super("Instructor with id " + instructorId + " was not found");
    }
}
