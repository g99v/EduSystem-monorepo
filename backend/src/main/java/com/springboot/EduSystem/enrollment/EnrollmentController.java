package com.springboot.EduSystem.enrollment;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/enrollments")
@PreAuthorize("hasRole('ADMIN')")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<EnrollmentDTO> create(
            @Valid @RequestBody EnrollmentCreateDTO dto
    ) {
        EnrollmentDTO createdEnrollment = enrollmentService.create(dto);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdEnrollment.id())
                .toUri();

        return ResponseEntity.created(location).body(createdEnrollment);
    }

    @GetMapping
    public ResponseEntity<List<EnrollmentDTO>> findAll(
            @RequestParam(required = false) @Positive Integer studentId,
            @RequestParam(required = false) @Positive Integer courseId
    ) {
        return ResponseEntity.ok(enrollmentService.findAll(studentId, courseId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentDTO> findById(
            @PathVariable @Positive Integer id
    ) {
        return ResponseEntity.ok(enrollmentService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnrollmentDTO> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody EnrollmentUpdateDTO dto
    ) {
        return ResponseEntity.ok(enrollmentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EnrollmentDTO> delete(
            @PathVariable @Positive Integer id
    ) {
        return ResponseEntity.ok(enrollmentService.delete(id));
    }
}
