package com.act.studentmanagmentsystem.entity;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class StudentCourseId implements Serializable {
    private Long student; // Matches the field name in StudentCourse.student
    private Long course;  // Matches the field name in StudentCourse.course

    // Default constructor
    public StudentCourseId() {}

    // Constructor
    public StudentCourseId(Long student, Long course) {
        this.student = student;
        this.course = course;
    }

    // Equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentCourseId that = (StudentCourseId) o;
        return Objects.equals(student, that.student) && Objects.equals(course, that.course);
    }

    @Override
    public int hashCode() {
        return Objects.hash(student, course);
    }
}