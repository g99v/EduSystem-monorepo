package com.springboot.EduSystem.course;

import com.springboot.EduSystem.instructor.Instructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CourseMapper {
    public CourseDTO toCourseDTO(Course course) {
        Objects.requireNonNull(course, "Course should not be null");

        Instructor instructor = course.getInstructor();
        return new CourseDTO(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getTeacherName(),
                instructor == null ? null : instructor.getId(),
                instructorName(instructor)
        );
    }

    public Course toCourse(CourseDTO dto) {
        Objects.requireNonNull(dto, "CourseDTO should not be null");

        return new Course(
                dto.name().trim(),
                normalizeNullable(dto.description()),
                dto.teacherName().trim()
        );
    }

    public void updateCourse(Course course, CourseDTO dto) {
        Objects.requireNonNull(course, "Course should not be null");
        Objects.requireNonNull(dto, "CourseDTO should not be null");

        course.setName(dto.name().trim());
        course.setDescription(normalizeNullable(dto.description()));
        course.setTeacherName(dto.teacherName().trim());
    }

    private String instructorName(Instructor instructor) {
        if (instructor == null) {
            return null;
        }

        return (instructor.getFirstName() + " " + instructor.getLastName()).trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
