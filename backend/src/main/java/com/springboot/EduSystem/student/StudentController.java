package com.springboot.EduSystem.student;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('ADMIN')")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> saveStudent(
            @Valid @RequestBody StudentDTO studentDto
    ){
        return studentService.saveStudent(studentDto);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> findAllStudent(){
        return studentService.findAllStudent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> findStudentById(@PathVariable int id){
        return studentService.findStudentById(id);
    }

    @GetMapping("/search/{name}")
    public ResponseEntity<List<StudentResponseDTO>> findStudentByName(
            @PathVariable String name
    ){
        return studentService.findStudentByName(name);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> updateStudentById(
            @PathVariable int id,
            @Valid @RequestBody StudentUpdateDTO studentDto
            ){
        return studentService.updateStudentById(id,studentDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> DeleteStudentById(
            @PathVariable int id
    ){
        return studentService.DeleteStudentById(id);
    }

}
