package com.springboot.EduSystem.announcement;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/courses/{courseId}/announcements")
public class CourseAnnouncementController {
    private final AnnouncementService announcementService;

    public CourseAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<AnnouncementDTO> create(
            @PathVariable @Positive Integer courseId,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication
    ) {
        AnnouncementDTO announcement = announcementService.create(
                courseId,
                request,
                authentication
        );

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/announcements/{id}")
                .buildAndExpand(announcement.id())
                .toUri();

        return ResponseEntity.created(location).body(announcement);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AnnouncementDTO>> findByCourse(
            @PathVariable @Positive Integer courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                announcementService.findByCourse(courseId, authentication)
        );
    }
}
