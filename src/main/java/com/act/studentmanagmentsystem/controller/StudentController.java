package com.act.studentmanagmentsystem.controller;

import com.act.studentmanagmentsystem.entity.User;
import com.act.studentmanagmentsystem.entity.Course;
import com.act.studentmanagmentsystem.entity.StudentCourse;
import com.act.studentmanagmentsystem.repository.UserRepository;
import com.act.studentmanagmentsystem.repository.CourseRepository;
import com.act.studentmanagmentsystem.repository.StudentCourseRepository;
import com.act.studentmanagmentsystem.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private StudentCourseRepository studentCourseRepository;
    @Autowired private JwtUtil jwtUtil;

    @GetMapping("/profile")
    public User getProfile(@RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        return userRepository.findByEmail(email);
    }

    @GetMapping("/courses")
    public List<Course> getCourses(@RequestHeader("Authorization") String token) {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
        User user = userRepository.findByEmail(email);
        List<StudentCourse> enrollments = studentCourseRepository.findByStudent(user);
        return enrollments.stream().map(StudentCourse::getCourse).collect(Collectors.toList());
    }
}