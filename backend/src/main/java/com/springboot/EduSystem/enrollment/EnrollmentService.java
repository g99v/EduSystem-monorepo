package com.springboot.EduSystem.enrollment;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.course.CourseRepository;
import com.springboot.EduSystem.exception.CourseNotFoundException;
import com.springboot.EduSystem.exception.EnrollmentAlreadyExistsException;
import com.springboot.EduSystem.exception.EnrollmentNotFoundException;
import com.springboot.EduSystem.exception.StudentNotFoundException;
import com.springboot.EduSystem.security.CourseAccessPolicy;
import com.springboot.EduSystem.student.Student;
import com.springboot.EduSystem.student.StudentRepository;

@Service
@Transactional
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseAccessPolicy accessPolicy;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            EnrollmentMapper enrollmentMapper,
            CourseAccessPolicy accessPolicy
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.accessPolicy = accessPolicy;
    }

    public EnrollmentDTO create(EnrollmentCreateDTO dto) {
        if (enrollmentRepository.existsByStudentIdAndCourseId(dto.studentId(), dto.courseId())) {
            throw new EnrollmentAlreadyExistsException(dto.studentId(), dto.courseId());
        }

        Student student = studentRepository.findById(dto.studentId())
                .orElseThrow(() -> new StudentNotFoundException(dto.studentId()));
        Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> new CourseNotFoundException(dto.courseId()));

        Enrollment enrollment = enrollmentMapper.toEnrollment(student, course);

        try {
            Enrollment savedEnrollment = enrollmentRepository.saveAndFlush(enrollment);
            return enrollmentMapper.toEnrollmentDTO(savedEnrollment);
        } catch (DataIntegrityViolationException exception) {
            throw new EnrollmentAlreadyExistsException(dto.studentId(), dto.courseId());
        }
    }

    public EnrollmentDTO enrollSelf(Integer courseId, Authentication authentication) {
        Student student = accessPolicy.requireStudent(authentication);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new EnrollmentAlreadyExistsException(student.getId(), courseId);
        }

        Enrollment enrollment = enrollmentMapper.toEnrollment(student, course);

        try {
            Enrollment savedEnrollment = enrollmentRepository.saveAndFlush(enrollment);
            return enrollmentMapper.toEnrollmentDTO(savedEnrollment);
        } catch (DataIntegrityViolationException exception) {
            // The unique constraint also covers concurrent double-submits.
            throw new EnrollmentAlreadyExistsException(student.getId(), courseId);
        }
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDTO> findAll(Integer studentId, Integer courseId) {
        List<Enrollment> enrollments;

        if (studentId != null && courseId != null) {
            enrollments = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                    .stream()
                    .toList();
        } else if (studentId != null) {
            enrollments = enrollmentRepository.findAllByStudentIdOrdered(studentId);
        } else if (courseId != null) {
            enrollments = enrollmentRepository.findAllByCourseIdOrdered(courseId);
        } else {
            enrollments = enrollmentRepository.findAllOrdered();
        }

        return enrollments.stream()
                .map(enrollmentMapper::toEnrollmentDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentDTO findById(Integer id) {
        return enrollmentMapper.toEnrollmentDTO(findEnrollment(id));
    }

    public EnrollmentDTO update(Integer id, EnrollmentUpdateDTO dto) {
        Enrollment enrollment = findEnrollment(id);
        enrollmentMapper.updateEnrollment(enrollment, dto);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        return enrollmentMapper.toEnrollmentDTO(savedEnrollment);
    }

    public EnrollmentDTO delete(Integer id) {
        Enrollment enrollment = findEnrollment(id);
        EnrollmentDTO deletedEnrollment = enrollmentMapper.toEnrollmentDTO(enrollment);
        enrollmentRepository.delete(enrollment);
        return deletedEnrollment;
    }

    private Enrollment findEnrollment(Integer id) {
        return enrollmentRepository.findOneById(id)
                .orElseThrow(() -> new EnrollmentNotFoundException(id));
    }
}
