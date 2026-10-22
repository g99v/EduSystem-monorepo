package com.springboot.EduSystem.lecture;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    boolean existsByStorageKey(String storageKey);

    @EntityGraph(attributePaths = "course")
    Optional<Lecture> findOneById(Integer id);

    @EntityGraph(attributePaths = "course")
    @Query("""
        SELECT lecture
        FROM Lecture lecture
        ORDER BY lecture.course.id ASC,
                 lecture.displayOrder ASC,
                 lecture.id ASC
        """)
    List<Lecture> findAllOrdered();

    @EntityGraph(attributePaths = "course")
    @Query("""
        SELECT lecture
        FROM Lecture lecture
        WHERE lecture.course.id = :courseId
        ORDER BY lecture.displayOrder ASC,
                 lecture.id ASC
        """)
    List<Lecture> findAllByCourseIdOrdered(
            @Param("courseId") Integer courseId
    );
}
