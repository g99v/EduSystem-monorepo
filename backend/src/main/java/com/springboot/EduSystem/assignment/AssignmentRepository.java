package com.springboot.EduSystem.assignment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {

    @EntityGraph(attributePaths = "course")
    Optional<Assignment> findOneById(Integer id);

    @EntityGraph(attributePaths = "course")
    @Query("""
        SELECT assignment
        FROM Assignment assignment
        ORDER BY assignment.course.id ASC,
                 assignment.displayOrder ASC,
                 assignment.id ASC
        """)
    List<Assignment> findAllOrdered();

    @EntityGraph(attributePaths = "course")
    @Query("""
        SELECT assignment
        FROM Assignment assignment
        WHERE assignment.course.id = :courseId
        ORDER BY assignment.displayOrder ASC,
                 assignment.id ASC
        """)
    List<Assignment> findAllByCourseIdOrdered(
            @Param("courseId") Integer courseId
    );
}
