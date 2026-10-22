package com.springboot.EduSystem.exception;

public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(Integer enrollmentId) {
        super("Enrollment with id " + enrollmentId + " was not found");
    }
}
