package com.springboot.EduSystem.announcement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    @EntityGraph(attributePaths = "course")
    Optional<Announcement> findOneById(Integer id);

    @EntityGraph(attributePaths = "course")
    @Query("""
        SELECT announcement
        FROM Announcement announcement
        WHERE announcement.course.id = :courseId
        ORDER BY announcement.createdAt DESC,
                 announcement.id DESC
        """)
    List<Announcement> findAllByCourseIdOrdered(
            @Param("courseId") Integer courseId
    );
}
