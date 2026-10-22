package com.springboot.EduSystem.school;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchoolDTO> create(
            @Valid @RequestBody SchoolDTO schoolDto){
        return schoolService.create(schoolDto);
    }

    @GetMapping
    public ResponseEntity<List<SchoolDTO>> getSchools(){
        return schoolService.getSchools();
    }

    //@GetMapping("/schools")
    public ResponseEntity<List<School>> getFullSchoolsObjs(){
        return schoolService.getFullSchoolsObjs();
    }
}
