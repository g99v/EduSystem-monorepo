package com.springboot.EduSystem.student;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    List<Student> findAllByfirstName(String p);

    Optional<Student> findByUser_EmailIgnoreCase(String email);
}
