package com.springboot.EduSystem.exception;

public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(Integer assignmentId) {
        super("Assignment with id " + assignmentId + " was not found");
    }
}
