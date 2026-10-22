package com.springboot.EduSystem.assignment;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.course.CourseRepository;
import com.springboot.EduSystem.exception.AssignmentNotFoundException;
import com.springboot.EduSystem.exception.CourseNotFoundException;
import com.springboot.EduSystem.security.CourseAccessPolicy;
import com.springboot.EduSystem.storage.DocumentFileValidator;
import com.springboot.EduSystem.storage.FileStorage;

@Service
@Transactional
public class AssignmentService {
    private static final String ASSIGNMENT_STORAGE_PREFIX = "assignments";

    private final AssignmentRepository assignments;
    private final AssignmentMapper assignmentMapper;
    private final CourseRepository courses;
    private final FileStorage fileStorage;
    private final DocumentFileValidator documentFileValidator;
    private final CourseAccessPolicy accessPolicy;
    private final SubmissionRepository submissions;

    public AssignmentService(
            AssignmentRepository assignments,
            AssignmentMapper assignmentMapper,
            CourseRepository courses,
            FileStorage fileStorage,
            DocumentFileValidator documentFileValidator,
            CourseAccessPolicy accessPolicy, SubmissionRepository submissions
    ) {
        this.assignments = assignments;
        this.assignmentMapper = assignmentMapper;
        this.courses = courses;
        this.fileStorage = fileStorage;
        this.documentFileValidator = documentFileValidator;
        this.accessPolicy = accessPolicy;
        this.submissions = submissions;
    }

    public AssignmentDTO upload(
            Integer courseId,
            String title,
            String assignmentInstructions,
            Integer maxGrade,
            Integer displayOrder,
            MultipartFile file,
            Authentication authentication
    ) {
        Course course = findCourse(courseId);
        accessPolicy.requireCourseManagement(course, authentication);

        DocumentFileValidator.ValidatedFile validatedFile =
                documentFileValidator.validate(file);
        FileStorage.StoredFile storedFile =
                fileStorage.store(file, ASSIGNMENT_STORAGE_PREFIX);

        try {
            Assignment assignment = new Assignment(
                    course,
                    title.trim(),
                    normalizeNullable(assignmentInstructions),
                    maxGrade,
                    validatedFile.originalFilename(),
                    storedFile.storageKey(),
                    validatedFile.contentType(),
                    storedFile.fileSize(),
                    displayOrder
            );
            Assignment savedAssignment = assignments.saveAndFlush(assignment);
            return assignmentMapper.toAssignmentDTO(savedAssignment);
        } catch (RuntimeException databaseException) {
            try {
                fileStorage.delete(storedFile.storageKey());
            } catch (RuntimeException cleanupException) {
                databaseException.addSuppressed(cleanupException);
            }
            throw databaseException;
        }
    }

    @Transactional(readOnly = true)
    public List<AssignmentDTO> findAll(Integer courseId, Authentication authentication) {
        List<Assignment> found;

        if (courseId == null) {
            accessPolicy.requireAdmin(authentication);
            found = assignments.findAllOrdered();
        } else {
            Course course = findCourse(courseId);
            accessPolicy.requireCourseReadAccess(course, authentication);
            found = assignments.findAllByCourseIdOrdered(courseId);
        }

        return found.stream()
                .map(assignmentMapper::toAssignmentDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDTO findById(Integer id, Authentication authentication) {
        Assignment assignment = findAssignment(id);
        accessPolicy.requireCourseReadAccess(assignment.getCourse(), authentication);
        return assignmentMapper.toAssignmentDTO(assignment);
    }

    @Transactional(readOnly = true)
    public AssignmentDownload download(Integer id, Authentication authentication) {
        Assignment assignment = findAssignment(id);
        accessPolicy.requireCourseReadAccess(assignment.getCourse(), authentication);

        Resource resource = fileStorage.load(assignment.getStorageKey());
        return new AssignmentDownload(
                resource,
                assignment.getOriginalFilename(),
                assignment.getContentType(),
                assignment.getFileSize()
        );
    }

    public AssignmentDTO update(Integer id, AssignmentUpdateDTO dto, Authentication authentication) {
        Assignment assignment = findAssignment(id);
        accessPolicy.requireCourseManagement(assignment.getCourse(), authentication);

        assignmentMapper.updateAssignment(assignment, dto);
        return assignmentMapper.toAssignmentDTO(assignments.save(assignment));
    }

    public AssignmentDTO delete(
            Integer id,
            Authentication authentication
    ) {
        Assignment assignment = findAssignment(id);
        accessPolicy.requireCourseManagement(
                assignment.getCourse(),
                authentication
        );

        AssignmentDTO deletedAssignment =
                assignmentMapper.toAssignmentDTO(assignment);

        String assignmentStorageKey =
                assignment.getStorageKey();

        List<Submission> assignmentSubmissions =
                submissions.findAllByAssignmentIdOrdered(id);

        List<String> submissionStorageKeys =
                assignmentSubmissions.stream()
                        .map(Submission::getStorageKey)
                        .toList();

        submissions.deleteAll(assignmentSubmissions);
        submissions.flush();

        assignments.delete(assignment);
        assignments.flush();

        for (String storageKey : submissionStorageKeys) {
            fileStorage.delete(storageKey);
        }

        fileStorage.delete(assignmentStorageKey);

        return deletedAssignment;
    }

    private Assignment findAssignment(Integer id) {
        return assignments.findOneById(id)
                .orElseThrow(() -> new AssignmentNotFoundException(id));
    }

    private Course findCourse(Integer id) {
        return courses.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record AssignmentDownload(
            Resource resource,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
    }
}
