package com.springboot.EduSystem.student;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.springboot.EduSystem.enrollment.Enrollment;
import com.springboot.EduSystem.school.School;
import com.springboot.EduSystem.studentprofile.StudentProfile;
import com.springboot.EduSystem.user.User;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="T_STUDENT")
public class Student {
    @Id
    @SequenceGenerator(
            name = "student_id_generator",
            sequenceName = "t_student_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "student_id_generator"
    )
    private Integer id;
    @Column(
            length = 20
    )
    private String firstName;
    private String lastName;
    private int age;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne(
            mappedBy = "student",
            cascade = CascadeType.ALL
    )
    private StudentProfile studentProfile;

    @ManyToOne
    @JoinColumn(
            name = "school_id"
    )
    @JsonBackReference
    private School school;

    @OneToMany(mappedBy = "student")
    private Set<Enrollment> enrollments = new HashSet<>();

    public Student() {
    }

    public Student(String firstName, String lastName, int age, User user) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.user = user;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }


    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Set<Enrollment> getEnrollments() {
        return enrollments;
    }

    public StudentProfile getStudentProfile() {
        return studentProfile;
    }

    public void setStudentProfile(StudentProfile studentProfile) {
        if (this.studentProfile == studentProfile) {
            return;
        }

        StudentProfile previousProfile = this.studentProfile;
        this.studentProfile = studentProfile;

        if (previousProfile != null && previousProfile.getStudent() == this) {
            previousProfile.setStudent(null);
        }
        if (studentProfile != null && studentProfile.getStudent() != this) {
            studentProfile.setStudent(this);
        }
    }

}
