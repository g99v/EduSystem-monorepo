package com.springboot.EduSystem.exception;

public class EnrollmentAlreadyExistsException extends RuntimeException {
    public EnrollmentAlreadyExistsException(Integer studentId, Integer courseId) {
        super("Student " + studentId + " is already enrolled in course " + courseId);
    }
}
