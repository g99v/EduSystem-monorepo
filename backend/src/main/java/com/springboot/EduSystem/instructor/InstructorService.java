package com.springboot.EduSystem.instructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springboot.EduSystem.user.Role;
import com.springboot.EduSystem.user.User;
import com.springboot.EduSystem.user.UserAccountService;

@Service
public class InstructorService {
    private final UserAccountService accounts;
    private final InstructorRepository instructors;

    public InstructorService(UserAccountService accounts, InstructorRepository instructors) {
        this.accounts = accounts;
        this.instructors = instructors;
    }

    @Transactional
    public InstructorResponse create(CreateInstructorRequest request) {
        User user = accounts.create(request.email(), request.password(), Role.INSTRUCTOR);
        Instructor instructor = new Instructor();
        instructor.setFirstName(request.firstName());
        instructor.setLastName(request.lastName());
        instructor.setUser(user);
        return InstructorResponse.from(instructors.save(instructor));
    }

    @Transactional(readOnly = true)
    public List<InstructorResponse> findAll() {
        return instructors.findAll().stream()
                .map(InstructorResponse::from)
                .toList();
    }
}
