package com.springboot.EduSystem.enrollment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);

    @EntityGraph(attributePaths = {"student", "course"})
    Optional<Enrollment> findOneById(Integer id);

    @EntityGraph(attributePaths = {"student", "course"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.student.id = :studentId
              AND enrollment.course.id = :courseId
            """)
    Optional<Enrollment> findByStudentIdAndCourseId(
            @Param("studentId") Integer studentId,
            @Param("courseId") Integer courseId
    );

    @EntityGraph(attributePaths = {"student", "course"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            ORDER BY enrollment.enrolledAt DESC
            """)
    List<Enrollment> findAllOrdered();

    @EntityGraph(attributePaths = {"student", "course"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.student.id = :studentId
            ORDER BY enrollment.enrolledAt DESC
            """)
    List<Enrollment> findAllByStudentIdOrdered(
            @Param("studentId") Integer studentId
    );

    @EntityGraph(attributePaths = {"student", "course"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.course.id = :courseId
            ORDER BY enrollment.enrolledAt DESC
            """)
    List<Enrollment> findAllByCourseIdOrdered(
            @Param("courseId") Integer courseId
    );
}
