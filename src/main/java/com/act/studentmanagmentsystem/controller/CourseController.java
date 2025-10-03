package com.act.studentmanagmentsystem.controller;

import com.act.studentmanagmentsystem.entity.Course;
import com.act.studentmanagmentsystem.entity.User;
import com.act.studentmanagmentsystem.repository.CourseRepository;
import com.act.studentmanagmentsystem.repository.UserRepository;
import com.act.studentmanagmentsystem.service.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course")
public class CourseController {
    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public CourseController(CourseRepository courseRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody CourseRequest request, @RequestHeader("Authorization") String token) {
        logger.info("Processing course creation request: {}", request.getName());
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User user = userRepository.findByEmail(email);
            if (user == null || !user.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to create course by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can create courses"));
            }

            Course course = new Course();
            course.setName(request.getName());
            course.setDescription(request.getDescription());
            course.setInstructor(request.getInstructor());
            course.setMaxEnrollment(request.getMaxEnrollment());
            course.setStatus("ACTIVE");
            courseRepository.save(course);
            logger.info("Course created successfully: {}", request.getName());
            return ResponseEntity.ok("Course created successfully");
        } catch (Exception e) {
            logger.error("Unexpected error creating course: {}", request.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Course creation failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody CourseRequest request, @RequestHeader("Authorization") String token) {
        logger.info("Processing course update request for id: {}", id);
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User user = userRepository.findByEmail(email);
            if (user == null || !user.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to update course by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can update courses"));
            }

            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));
            course.setName(request.getName());
            course.setDescription(request.getDescription());
            course.setInstructor(request.getInstructor());
            course.setMaxEnrollment(request.getMaxEnrollment());
            courseRepository.save(course);
            logger.info("Course updated successfully: {}", id);
            return ResponseEntity.ok("Course updated successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Course not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating course: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Course update failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        logger.info("Processing course deletion request for id: {}", id);
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User user = userRepository.findByEmail(email);
            if (user == null || !user.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to delete course by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can delete courses"));
            }

            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));
            courseRepository.delete(course);
            logger.info("Course deleted successfully: {}", id);
            return ResponseEntity.ok("Course deleted successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Course not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting course: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Course deletion failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getCourses() {
        try {
            List<CourseResponse> courses = courseRepository.findAll().stream()
                    .map(course -> new CourseResponse(
                            course.getId(),
                            course.getName(),
                            course.getDescription(),
                            course.getInstructor(),
                            course.getMaxEnrollment(),
                            course.getStatus(),
                            course.getStudents().size(),
                            course.getStudents().stream().map(User::getId).collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Unexpected error fetching courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch courses: " + e.getMessage()));
        }
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody EnrollRequest request, @RequestHeader("Authorization") String token) {
        logger.info("Processing enrollment request for course: {}", request.getCourseId());
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User student = userRepository.findByEmail(email);
            if (student == null || !student.getRole().name().equals("STUDENT")) {
                logger.warn("Unauthorized attempt to enroll by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only STUDENT can enroll"));
            }

            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));
            if (course.getStudents().contains(student)) {
                logger.warn("Student already enrolled in course: {}", request.getCourseId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, "Student already enrolled in course"));
            }
            if (course.getStudents().size() >= course.getMaxEnrollment()) {
                logger.warn("Course enrollment limit reached: {}", request.getCourseId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, "Course enrollment limit reached"));
            }

            student.getCourses().add(course);
            course.getStudents().add(student);
            userRepository.save(student);
            courseRepository.save(course);
            logger.info("Student enrolled successfully in course: {}", request.getCourseId());
            return ResponseEntity.ok("Enrolled successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Course not found: {}", request.getCourseId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error enrolling in course: {}", request.getCourseId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Enrollment failed: " + e.getMessage()));
        }
    }
}

class CourseRequest {
    private String name;
    private String description;
    private String instructor;
    private int maxEnrollment;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public int getMaxEnrollment() { return maxEnrollment; }
    public void setMaxEnrollment(int maxEnrollment) { this.maxEnrollment = maxEnrollment; }
}

class EnrollRequest {
    private Long courseId;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
}

class CourseResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String instructor;
    private final int maxEnrollment;
    private final String status;
    private final int enrollmentCount;
    private final List<Long> studentIds;

    public CourseResponse(Long id, String name, String description, String instructor, int maxEnrollment, String status, int enrollmentCount, List<Long> studentIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.instructor = instructor;
        this.maxEnrollment = maxEnrollment;
        this.status = status;
        this.enrollmentCount = enrollmentCount;
        this.studentIds = studentIds;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getInstructor() { return instructor; }
    public int getMaxEnrollment() { return maxEnrollment; }
    public String getStatus() { return status; }
    public int getEnrollmentCount() { return enrollmentCount; }
    public List<Long> getStudentIds() { return studentIds; }
}

