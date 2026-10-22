package com.springboot.EduSystem.course;

import com.springboot.EduSystem.exception.CourseNotFoundException;
import com.springboot.EduSystem.exception.InstructorNotFoundException;
import com.springboot.EduSystem.instructor.Instructor;
import com.springboot.EduSystem.instructor.InstructorRepository;
import com.springboot.EduSystem.security.CourseAccessPolicy;
import com.springboot.EduSystem.student.Student;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final InstructorRepository instructorRepository;
    private final CourseAccessPolicy accessPolicy;

    public CourseService(CourseRepository courseRepository,
                         CourseMapper courseMapper,
                         InstructorRepository instructorRepository,
                         CourseAccessPolicy accessPolicy) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
        this.instructorRepository = instructorRepository;
        this.accessPolicy = accessPolicy;
    }

    public CourseDTO create(CourseDTO dto) {
        Course course = courseMapper.toCourse(dto);
        course.setInstructor(resolveInstructor(dto.instructorId()));
        Course savedCourse = courseRepository.save(course);
        return courseMapper.toCourseDTO(savedCourse);
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> findAll(String name) {
        List<Course> courses;

        if (name == null || name.isBlank()) {
            courses = courseRepository.findAllByOrderByNameAsc();
        } else {
            courses = courseRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
        }

        return toDTOs(courses);
    }


    @Transactional(readOnly = true)
    public List<CourseDTO> findForCurrentUser(Authentication authentication) {
        if (accessPolicy.isAdmin(authentication)) {
            return toDTOs(courseRepository.findAllByOrderByNameAsc());
        }

        if (accessPolicy.isInstructor(authentication)) {
            Instructor instructor = accessPolicy.requireInstructor(authentication);
            return toDTOs(courseRepository.findAllByInstructorIdOrdered(instructor.getId()));
        }

        Student student = accessPolicy.requireStudent(authentication);
        return toDTOs(courseRepository.findAllByStudentIdOrdered(student.getId()));
    }

    @Transactional(readOnly = true)
    public Optional<CourseDTO> findById(Integer id) {
        return courseRepository.findById(id)
                .map(courseMapper::toCourseDTO);
    }

    public Optional<CourseDTO> update(Integer id, CourseDTO dto) {
        return courseRepository.findById(id)
                .map(course -> {
                    courseMapper.updateCourse(course, dto);
                    if (dto.instructorId() != null) {
                        course.setInstructor(resolveInstructor(dto.instructorId()));
                    }
                    Course savedCourse = courseRepository.save(course);
                    return courseMapper.toCourseDTO(savedCourse);
                });
    }

    public CourseDTO delete(Integer id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));

        CourseDTO deletedCourse = courseMapper.toCourseDTO(course);
        courseRepository.delete(course);

        return deletedCourse;
    }

    private Instructor resolveInstructor(Integer instructorId) {
        if (instructorId == null) {
            return null;
        }

        return instructorRepository.findById(instructorId)
                .orElseThrow(() -> new InstructorNotFoundException(instructorId));
    }

    private List<CourseDTO> toDTOs(List<Course> courses) {
        return courses.stream()
                .map(courseMapper::toCourseDTO)
                .toList();
    }
}
