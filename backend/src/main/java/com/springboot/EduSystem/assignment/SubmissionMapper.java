package com.springboot.EduSystem.assignment;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.springboot.EduSystem.student.Student;

@Component
public class SubmissionMapper {
    public SubmissionDTO toSubmissionDTO(Submission submission) {
        Objects.requireNonNull(submission, "Submission should not be null");

        Student student = submission.getStudent();
        String studentName = (student.getFirstName() + " " + student.getLastName()).trim();

        return new SubmissionDTO(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                student.getId(),
                studentName,
                submission.getOriginalFilename(),
                submission.getContentType(),
                submission.getFileSize(),
                submission.getSubmittedAt(),
                submission.getGrade(),
                submission.getFeedback(),
                submission.getGradedAt()
        );
    }

    public void applyGrade(Submission submission, SubmissionGradeDTO dto) {
        Objects.requireNonNull(submission, "Submission should not be null");
        Objects.requireNonNull(dto, "SubmissionGradeDTO should not be null");

        submission.setGrade(dto.grade());
        submission.setFeedback(normalizeNullable(dto.feedback()));
        submission.setGradedAt(Instant.now());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
