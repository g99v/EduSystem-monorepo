package com.springboot.EduSystem.studentprofile;

import com.springboot.EduSystem.student.Student;
import jakarta.persistence.*;

@Entity
public class StudentProfile {
    @Id
    @SequenceGenerator(
            name = "student_profile_id_generator",
            sequenceName = "student_profile_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "student_profile_id_generator"
    )
    private Integer id;
    private String bio;

    @OneToOne
    @JoinColumn(
            name = "student_id"
    )
    private Student student;

    public StudentProfile() {
    }

    public StudentProfile(String bio) {
        this.bio = bio;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        if (this.student == student) {
            return;
        }

        Student previousStudent = this.student;
        this.student = student;

        if (previousStudent != null && previousStudent.getStudentProfile() == this) {
            previousStudent.setStudentProfile(null);
        }
        if (student != null && student.getStudentProfile() != this) {
            student.setStudentProfile(this);
        }
    }
}
