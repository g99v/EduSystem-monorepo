package com.springboot.EduSystem.lecture;

import com.springboot.EduSystem.course.Course;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "lecture",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecture_storage_key",
                columnNames = "storage_key"
        )
)
public class Lecture {
    @Id
    @SequenceGenerator(
            name = "lecture_id_generator",
            sequenceName = "lecture_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "lecture_id_generator"
    )
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

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

    public Lecture() {
    }

    public Lecture(
            Course course,
            String title,
            String originalFilename,
            String storageKey,
            String contentType,
            long fileSize,
            int displayOrder
    ) {
        this.course = course;
        this.title = title;
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
