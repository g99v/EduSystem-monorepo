package com.springboot.EduSystem.student;

import org.springframework.stereotype.Service;

@Service
public class StudentMapper {
    public StudentResponseDTO toStudentResponseDTO(Student student){
        return new StudentResponseDTO(student.getFirstName(), student.getLastName(), student.getUser().getEmail());
    }
}
