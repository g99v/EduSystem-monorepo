package com.springboot.EduSystem.exception;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(Integer studentId) {
        super("Student with id " + studentId + " was not found");
    }
}
