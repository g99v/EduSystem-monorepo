package com.springboot.EduSystem.assignment;

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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/courses/{courseId}/assignments")
public class CourseAssignmentController {
    private final AssignmentService assignmentService;

    public CourseAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<AssignmentDTO> upload(
            @PathVariable @Positive Integer courseId,
            @RequestParam @NotBlank @Size(max = 200) String title,
            @RequestParam(required = false) @Size(max = 10000) String assignmentInstructions,
            @RequestParam
            @NotNull
            @Min(value = Assignment.MIN_MAX_GRADE)
            @Max(value = Assignment.MAX_MAX_GRADE)
            Integer maxGrade,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer displayOrder,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        AssignmentDTO assignment = assignmentService.upload(
                courseId,
                title,
                assignmentInstructions,
                maxGrade,
                displayOrder,
                file,
                authentication
        );

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/assignments/{id}")
                .buildAndExpand(assignment.id())
                .toUri();

        return ResponseEntity.created(location).body(assignment);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AssignmentDTO>> findByCourse(
            @PathVariable @Positive Integer courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(assignmentService.findAll(courseId, authentication));
    }
}
