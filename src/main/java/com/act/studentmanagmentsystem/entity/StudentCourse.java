package com.act.studentmanagmentsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "student_courses")
@IdClass(StudentCourseId.class)
public class StudentCourse {
    @Id
    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Id
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDateTime enrolledAt;
}