package com.springboot.EduSystem.assignment;

import java.time.Instant;

import com.springboot.EduSystem.course.Course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_storage_key",
                columnNames = "storage_key"
        )
)
public class Assignment {
    public static final int MIN_MAX_GRADE = 0;
    public static final int MAX_MAX_GRADE = 25;

    @Id
    @SequenceGenerator(
            name = "assignment_id_generator",
            sequenceName = "assignment_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "assignment_id_generator"
    )
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "assignment_instructions", columnDefinition = "TEXT")
    private String assignmentInstructions;

    @Column(name = "max_grade", nullable = false)
    private int maxGrade;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public Assignment() {
    }

    public Assignment(
            Course course,
            String title,
            String assignmentInstructions,
            int maxGrade,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSize,
            int displayOrder
    ) {
        this.course = course;
        this.title = title;
        this.assignmentInstructions = assignmentInstructions;
        this.maxGrade = maxGrade;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.displayOrder = displayOrder;
        this.uploadedAt = Instant.now();
    }

    @PrePersist
    private void applyDefaults() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAssignmentInstructions() {
        return assignmentInstructions;
    }

    public void setAssignmentInstructions(String assignmentInstructions) {
        this.assignmentInstructions = assignmentInstructions;
    }

    public int getMaxGrade() {
        return maxGrade;
    }

    public void setMaxGrade(int maxGrade) {
        this.maxGrade = maxGrade;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
