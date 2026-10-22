package com.springboot.EduSystem.student;

import com.springboot.EduSystem.auth.AuthService;
import com.springboot.EduSystem.auth.RegisterStudentRequest;
import com.springboot.EduSystem.exception.EmailAlreadyExistsException;
import com.springboot.EduSystem.school.SchoolRepository;
import com.springboot.EduSystem.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class StudentService {
    private final StudentRepository students;
    private final SchoolRepository schools;
    private final StudentMapper mapper;
    private final UserRepository users;
    private final AuthService authService;

    public StudentService(StudentRepository students, StudentMapper mapper, SchoolRepository schools,
                          UserRepository users, AuthService authService) {
        this.students = students;
        this.mapper = mapper;
        this.schools = schools;
        this.users = users;
        this.authService = authService;
    }

    public ResponseEntity<StudentResponseDTO> saveStudent(StudentDTO dto) {
        if (!schools.existsById(dto.schoolId())) {
            return ResponseEntity.notFound().build();
        }
        Student student = authService.registerStudent(new RegisterStudentRequest(
                dto.firstName(), dto.lastName(), dto.email(), dto.password(), dto.age(), dto.schoolId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toStudentResponseDTO(student));
    }

    public ResponseEntity<List<StudentResponseDTO>> findAllStudent() {
        return ResponseEntity.ok(students.findAll().stream().map(mapper::toStudentResponseDTO).toList());
    }

    public ResponseEntity<StudentResponseDTO> findStudentById(int id) {
        return students.findById(id).map(mapper::toStudentResponseDTO)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public ResponseEntity<List<StudentResponseDTO>> findStudentByName(String name) {
        return ResponseEntity.ok(students.findAllByfirstName(name).stream()
                .map(mapper::toStudentResponseDTO).toList());
    }

    @Transactional
    public ResponseEntity<StudentResponseDTO> updateStudentById(int id, StudentUpdateDTO dto) {
        Optional<Student> existing = students.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student student = existing.get();
        if (dto.schoolId() != null) {
            var school = schools.findById(dto.schoolId());
            if (school.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            student.setSchool(school.get());
        }
        if (dto.firstName() != null) student.setFirstName(dto.firstName());
        if (dto.lastName() != null) student.setLastName(dto.lastName());
        if (dto.email() != null) {
            String email = dto.email().trim().toLowerCase(Locale.ROOT);
            if (users.existsByEmailIgnoreCaseAndIdNot(email, student.getUser().getId())) {
                throw new EmailAlreadyExistsException();
            }
            student.getUser().setEmail(email);
            try {
                users.saveAndFlush(student.getUser());
            } catch (DataIntegrityViolationException exception) {
                throw new EmailAlreadyExistsException();
            }
        }
        return ResponseEntity.ok(mapper.toStudentResponseDTO(students.save(student)));
    }

    @Transactional
    public ResponseEntity<StudentResponseDTO> DeleteStudentById(int id) {
        Optional<Student> existing = students.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student student = existing.get();
        StudentResponseDTO response = mapper.toStudentResponseDTO(student);
        students.delete(student);
        students.flush();
        users.delete(student.getUser());
        return ResponseEntity.ok(response);
    }
}
