package com.springboot.EduSystem.lecture;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/courses/{courseId}/lectures")
public class CourseLectureController {
    private final LectureService lectureService;

    public CourseLectureController(LectureService lectureService) {
        this.lectureService = lectureService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<LectureDTO> upload(
            @PathVariable @Positive Integer courseId,
            @RequestParam @NotBlank @Size(max = 200) String title,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer displayOrder,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        LectureDTO lecture = lectureService.upload(
                courseId,
                title,
                displayOrder,
                file,
                authentication
        );

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/lectures/{id}")
                .buildAndExpand(lecture.id())
                .toUri();

        return ResponseEntity.created(location).body(lecture);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LectureDTO>> findByCourse(
            @PathVariable @Positive Integer courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(lectureService.findAll(courseId, authentication));
    }
}
