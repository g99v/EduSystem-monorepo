package com.springboot.EduSystem.announcement;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnnouncementDTO> findById(
            @PathVariable @Positive Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                announcementService.findById(id, authentication)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<AnnouncementDTO> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody AnnouncementRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                announcementService.update(id, request, authentication)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<AnnouncementDTO> delete(
            @PathVariable @Positive Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                announcementService.delete(id, authentication)
        );
    }
}
