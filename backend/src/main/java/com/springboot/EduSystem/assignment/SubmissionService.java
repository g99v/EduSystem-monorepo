package com.springboot.EduSystem.assignment;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.EduSystem.exception.AssignmentNotFoundException;
import com.springboot.EduSystem.exception.GradeOutOfRangeException;
import com.springboot.EduSystem.exception.SubmissionNotFoundException;
import com.springboot.EduSystem.security.CourseAccessPolicy;
import com.springboot.EduSystem.storage.DocumentFileValidator;
import com.springboot.EduSystem.storage.FileStorage;
import com.springboot.EduSystem.student.Student;

@Service
@Transactional
public class SubmissionService {
    private static final String SUBMISSION_STORAGE_PREFIX = "submissions";

    private final SubmissionRepository submissions;
    private final SubmissionMapper submissionMapper;
    private final AssignmentRepository assignments;
    private final FileStorage fileStorage;
    private final DocumentFileValidator documentFileValidator;
    private final CourseAccessPolicy accessPolicy;

    public SubmissionService(
            SubmissionRepository submissions,
            SubmissionMapper submissionMapper,
            AssignmentRepository assignments,
            FileStorage fileStorage,
            DocumentFileValidator documentFileValidator,
            CourseAccessPolicy accessPolicy
    ) {
        this.submissions = submissions;
        this.submissionMapper = submissionMapper;
        this.assignments = assignments;
        this.fileStorage = fileStorage;
        this.documentFileValidator = documentFileValidator;
        this.accessPolicy = accessPolicy;
    }

    public SubmissionDTO upload(Integer assignmentId, MultipartFile file, Authentication authentication) {
        Assignment assignment = findAssignment(assignmentId);
        Student student = accessPolicy.requireEnrolledStudent(assignment.getCourse(), authentication);

        DocumentFileValidator.ValidatedFile validatedFile =
                documentFileValidator.validate(file);
        FileStorage.StoredFile storedFile =
                fileStorage.store(file, SUBMISSION_STORAGE_PREFIX);

        try {
            Submission submission = new Submission(
                    assignment,
                    student,
                    validatedFile.originalFilename(),
                    storedFile.storageKey(),
                    validatedFile.contentType(),
                    storedFile.fileSize()
            );
            Submission savedSubmission = submissions.saveAndFlush(submission);
            return submissionMapper.toSubmissionDTO(savedSubmission);
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
    public List<SubmissionDTO> findAllByAssignment(Integer assignmentId, Authentication authentication) {
        Assignment assignment = findAssignment(assignmentId);
        accessPolicy.requireCourseManagement(assignment.getCourse(), authentication);

        return submissions.findAllByAssignmentIdOrdered(assignmentId).stream()
                .map(submissionMapper::toSubmissionDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionDTO> findMine(Integer assignmentId, Authentication authentication) {
        Assignment assignment = findAssignment(assignmentId);
        Student student = accessPolicy.requireEnrolledStudent(assignment.getCourse(), authentication);

        return submissions
                .findAllByAssignmentIdAndStudentIdOrdered(assignmentId, student.getId()).stream()
                .map(submissionMapper::toSubmissionDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionDTO findById(Integer id, Authentication authentication) {
        Submission submission = findSubmission(id);
        requireSubmissionReadAccess(submission, authentication);
        return submissionMapper.toSubmissionDTO(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionDownload download(Integer id, Authentication authentication) {
        Submission submission = findSubmission(id);
        requireSubmissionReadAccess(submission, authentication);

        Resource resource = fileStorage.load(submission.getStorageKey());
        return new SubmissionDownload(
                resource,
                submission.getOriginalFilename(),
                submission.getContentType(),
                submission.getFileSize()
        );
    }

    public SubmissionDTO grade(Integer id, SubmissionGradeDTO dto, Authentication authentication) {
        Submission submission = findSubmission(id);
        accessPolicy.requireCourseManagement(submission.getAssignment().getCourse(), authentication);

        int maxGrade = submission.getAssignment().getMaxGrade();
        if (dto.grade() > maxGrade) {
            throw new GradeOutOfRangeException(dto.grade(), maxGrade);
        }

        submissionMapper.applyGrade(submission, dto);
        return submissionMapper.toSubmissionDTO(submissions.save(submission));
    }

    public SubmissionDTO delete(Integer id, Authentication authentication) {
        Submission submission = findSubmission(id);

        if (!accessPolicy.isAdmin(authentication)) {
            Student student = accessPolicy.requireStudent(authentication);
            if (!submission.getStudent().getId().equals(student.getId())) {
                throw new AccessDeniedException("You may only withdraw your own submission");
            }
        }

        SubmissionDTO deletedSubmission = submissionMapper.toSubmissionDTO(submission);
        String storageKey = submission.getStorageKey();

        submissions.delete(submission);
        submissions.flush();
        fileStorage.delete(storageKey);

        return deletedSubmission;
    }

    private Submission findSubmission(Integer id) {
        return submissions.findOneById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
    }

    private Assignment findAssignment(Integer id) {
        return assignments.findOneById(id)
                .orElseThrow(() -> new AssignmentNotFoundException(id));
    }

    private void requireSubmissionReadAccess(Submission submission, Authentication authentication) {
        if (accessPolicy.isAdmin(authentication)) {
            return;
        }

        if (accessPolicy.isInstructor(authentication)) {
            accessPolicy.requireCourseManagement(
                    submission.getAssignment().getCourse(),
                    authentication
            );
            return;
        }

        Student student = accessPolicy.requireStudent(authentication);
        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("You may only read your own submission");
        }
    }

    public record SubmissionDownload(
            Resource resource,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
    }
}
