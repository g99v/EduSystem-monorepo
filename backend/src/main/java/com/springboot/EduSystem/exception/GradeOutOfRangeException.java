package com.springboot.EduSystem.exception;

public class GradeOutOfRangeException extends RuntimeException {
    public GradeOutOfRangeException(Integer grade, int maxGrade) {
        super("Grade " + grade + " exceeds the assignment maximum of " + maxGrade);
    }
}
