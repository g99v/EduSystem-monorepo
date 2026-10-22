package com.springboot.EduSystem.exception;

public class SubmissionNotFoundException extends RuntimeException {
    public SubmissionNotFoundException(Integer submissionId) {
        super("Submission with id " + submissionId + " was not found");
    }

    public SubmissionNotFoundException(Integer assignmentId, Integer studentId) {
        super("No submission was found for assignment " + assignmentId
                + " by student " + studentId);
    }
}
