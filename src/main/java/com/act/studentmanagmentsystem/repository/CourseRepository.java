package com.act.studentmanagmentsystem.repository;

import com.act.studentmanagmentsystem.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    long countByStatus(String active);
}