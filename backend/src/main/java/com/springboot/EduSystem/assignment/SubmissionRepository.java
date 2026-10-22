package com.springboot.EduSystem.assignment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Integer> {

    @EntityGraph(attributePaths = {"assignment", "assignment.course", "student", "student.user"})
    Optional<Submission> findOneById(Integer id);

    @EntityGraph(attributePaths = {"assignment", "assignment.course", "student", "student.user"})
    @Query("""
        SELECT submission
        FROM Submission submission
        WHERE submission.assignment.id = :assignmentId
        ORDER BY submission.submittedAt DESC,
                 submission.id DESC
        """)
    List<Submission> findAllByAssignmentIdOrdered(
            @Param("assignmentId") Integer assignmentId
    );

    @EntityGraph(attributePaths = {"assignment", "assignment.course", "student", "student.user"})
    @Query("""
        SELECT submission
        FROM Submission submission
        WHERE submission.assignment.id = :assignmentId
          AND submission.student.id = :studentId
        ORDER BY submission.submittedAt DESC,
                 submission.id DESC
        """)
    List<Submission> findAllByAssignmentIdAndStudentIdOrdered(
            @Param("assignmentId") Integer assignmentId,
            @Param("studentId") Integer studentId
    );

    boolean existsByAssignmentId(Integer assignmentId);
}
