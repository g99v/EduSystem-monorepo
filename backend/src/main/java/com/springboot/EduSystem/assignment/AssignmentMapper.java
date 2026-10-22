package com.springboot.EduSystem.assignment;

import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class AssignmentMapper {
    public AssignmentDTO toAssignmentDTO(Assignment assignment) {
        Objects.requireNonNull(assignment, "Assignment should not be null");

        return new AssignmentDTO(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getName(),
                assignment.getTitle(),
                assignment.getAssignmentInstructions(),
                assignment.getMaxGrade(),
                assignment.getOriginalFilename(),
                assignment.getContentType(),
                assignment.getFileSize(),
                assignment.getDisplayOrder(),
                assignment.getUploadedAt()
        );
    }

    public void updateAssignment(Assignment assignment, AssignmentUpdateDTO dto) {
        Objects.requireNonNull(assignment, "Assignment should not be null");
        Objects.requireNonNull(dto, "AssignmentUpdateDTO should not be null");

        assignment.setTitle(dto.title().trim());
        assignment.setAssignmentInstructions(normalizeNullable(dto.assignmentInstructions()));
        assignment.setMaxGrade(dto.maxGrade());
        assignment.setDisplayOrder(dto.displayOrder());
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
