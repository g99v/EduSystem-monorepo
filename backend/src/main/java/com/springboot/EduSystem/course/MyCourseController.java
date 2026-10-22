package com.springboot.EduSystem.course;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/courses")
public class MyCourseController {
    private final CourseService courseService;

    public MyCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseDTO>> findMine(Authentication authentication) {
        return ResponseEntity.ok(courseService.findForCurrentUser(authentication));
    }
}
