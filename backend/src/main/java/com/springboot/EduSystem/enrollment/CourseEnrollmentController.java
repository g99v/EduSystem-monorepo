package com.springboot.EduSystem.enrollment;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.constraints.Positive;


@Validated
@RestController
@RequestMapping("/api/courses/{courseId}/enrollments")
public class CourseEnrollmentController {
    private final EnrollmentService enrollmentService;

    public CourseEnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentDTO> enrollSelf(
            @PathVariable @Positive Integer courseId,
            Authentication authentication
    ) {
        EnrollmentDTO enrollment = enrollmentService.enrollSelf(courseId, authentication);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/enrollments/{id}")
                .buildAndExpand(enrollment.id())
                .toUri();

        return ResponseEntity.created(location).body(enrollment);
    }
}
