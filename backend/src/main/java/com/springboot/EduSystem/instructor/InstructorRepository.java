package com.springboot.EduSystem.instructor;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorRepository extends JpaRepository<Instructor, Integer> {
    Optional<Instructor> findByUser_EmailIgnoreCase(String email);
}
