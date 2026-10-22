package com.springboot.EduSystem.school;

import org.springframework.stereotype.Service;

@Service
public class SchoolMapper {
    public SchoolDTO toSchoolDto(School school){
        return new SchoolDTO(school.getName());
    }
    public School toSchool(SchoolDTO dto){
        return new School(dto.name());
    }

}
