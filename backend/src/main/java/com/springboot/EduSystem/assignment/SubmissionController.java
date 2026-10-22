package com.springboot.EduSystem.assignment;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api")
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping(
            value = "/assignments/{assignmentId}/submissions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubmissionDTO> submit(
            @PathVariable @Positive Integer assignmentId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        SubmissionDTO submission = submissionService.upload(assignmentId, file, authentication);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/submissions/{id}")
                .buildAndExpand(submission.id())
                .toUri();

        return ResponseEntity.created(location).body(submission);
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<SubmissionDTO>> findByAssignment(
            @PathVariable @Positive Integer assignmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                submissionService.findAllByAssignment(assignmentId, authentication)
        );
    }

    @GetMapping("/assignments/{assignmentId}/submissions/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<SubmissionDTO>> findMine(
            @PathVariable @Positive Integer assignmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(submissionService.findMine(assignmentId, authentication));
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubmissionDTO> findById(
            @PathVariable @Positive Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(submissionService.findById(id, authentication));
    }

    @GetMapping("/submissions/{id}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(
            @PathVariable @Positive Integer id,
            Authentication authentication
    ) {
        SubmissionService.SubmissionDownload download =
                submissionService.download(id, authentication);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.resource());
    }

    @PutMapping("/submissions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<SubmissionDTO> grade(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody SubmissionGradeDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(submissionService.grade(id, dto, authentication));
    }

    @DeleteMapping("/submissions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubmissionDTO> delete(
            @PathVariable @Positive Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(submissionService.delete(id, authentication));
    }
}
