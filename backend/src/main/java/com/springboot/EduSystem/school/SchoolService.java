package com.springboot.EduSystem.school;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SchoolService {
    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    public SchoolService(SchoolRepository schoolRepository, SchoolMapper schoolMapper) {
        this.schoolRepository = schoolRepository;
        this.schoolMapper = schoolMapper;
    }

    public ResponseEntity<SchoolDTO> create(SchoolDTO schoolDto){
        var school = schoolMapper.toSchool(schoolDto);
        schoolRepository.save(school);

        return new ResponseEntity<>(schoolDto, HttpStatus.CREATED);
    }

    public ResponseEntity<List<SchoolDTO>> getSchools(){
        List<SchoolDTO> schools = schoolRepository.findAll()
                .stream()
                .map(schoolMapper::toSchoolDto)
                .toList();

        return new ResponseEntity<>(schools,HttpStatus.OK);
    }
    public ResponseEntity<List<School>> getFullSchoolsObjs(){
        List<School> schools = schoolRepository.findAll();

        return new ResponseEntity<>(schools,HttpStatus.OK);
    }


}
