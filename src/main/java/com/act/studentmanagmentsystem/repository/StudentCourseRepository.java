package com.act.studentmanagmentsystem.repository;

import com.act.studentmanagmentsystem.entity.StudentCourse;
import com.act.studentmanagmentsystem.entity.StudentCourseId;
import com.act.studentmanagmentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentCourseRepository extends JpaRepository<StudentCourse, StudentCourseId> {
    List<StudentCourse> findByStudent(User student);
}