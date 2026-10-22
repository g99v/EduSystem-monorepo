package com.springboot.EduSystem.course;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    @EntityGraph(attributePaths = "instructor")
    List<Course> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "instructor")
    List<Course> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    @EntityGraph(attributePaths = "instructor")
    @Query("""
        SELECT course
        FROM Course course
        WHERE course.instructor.id = :instructorId
        ORDER BY course.name ASC
        """)
    List<Course> findAllByInstructorIdOrdered(@Param("instructorId") Integer instructorId);

    @EntityGraph(attributePaths = "instructor")
    @Query("""
        SELECT course
        FROM Enrollment enrollment
        JOIN enrollment.course course
        WHERE enrollment.student.id = :studentId
        ORDER BY course.name ASC
        """)
    List<Course> findAllByStudentIdOrdered(@Param("studentId") Integer studentId);
}
