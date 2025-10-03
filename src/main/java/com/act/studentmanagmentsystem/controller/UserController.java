package com.act.studentmanagmentsystem.controller;


import com.act.studentmanagmentsystem.entity.Course;
import com.act.studentmanagmentsystem.entity.Role;
import com.act.studentmanagmentsystem.entity.User;
import com.act.studentmanagmentsystem.repository.CourseRepository;
import com.act.studentmanagmentsystem.repository.UserRepository;
import com.act.studentmanagmentsystem.service.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserController(UserRepository userRepository, CourseRepository courseRepository, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request, @RequestHeader("Authorization") String token) {
        logger.info("Processing profile update request");
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User user = userRepository.findByEmail(email);
            if (user == null) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND, "User not found"));
            }

            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName() != null) user.setLastName(request.getLastName());
            if (request.getPhone() != null) user.setPhone(request.getPhone());
            if (request.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setMustChangePassword(false);
            }

            userRepository.save(user);
            logger.info("Profile updated successfully for email: {}", email);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (Exception e) {
            logger.error("Unexpected error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update profile: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/create")
    public ResponseEntity<?> createUserByAdmin(@RequestBody AdminCreateUserRequest request, @RequestHeader("Authorization") String token) {
        logger.info("Processing admin create user request for email: {}", request.getEmail());
        try {
            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User admin = userRepository.findByEmail(adminEmail);
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to create user by email: {}", adminEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can create users"));
            }

            Role role;
            try {
                role = Role.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid role provided: {}", request.getRole());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid role: " + request.getRole()));
            }

            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode("changeMe123"));
            user.setRole(role);
            user.setMustChangePassword(true);
            user.setPhone(request.getPhone());
            user.setGpa(request.getGpa());
            user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
            user.setCreatedAt(LocalDateTime.now());

            userRepository.save(user);
            logger.info("User created successfully by admin: {}", request.getEmail());
            return ResponseEntity.ok("User created successfully with default password 'changeMe123'. The user must change it on first login.");
        } catch (DataIntegrityViolationException e) {
            logger.warn("User creation failed: Email already exists: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT, "Email already exists: " + request.getEmail()));
        } catch (Exception e) {
            logger.error("Unexpected error creating user for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "User creation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/students")
    public ResponseEntity<?> getStudents(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minGpa,
            @RequestParam(required = false) Double maxGpa,
            @RequestParam(required = false) Long courseId) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User admin = userRepository.findByEmail(email);
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to view students by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can view students"));
            }

            Pageable pageable = PageRequest.of(page, size);
            Specification<User> spec = Specification.where((root, query, cb) ->
                    cb.equal(root.get("role"), Role.STUDENT));

            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("firstName")), searchPattern),
                        cb.like(cb.lower(root.get("lastName")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern)
                ));
            }

            if (status != null && !status.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
            }

            if (minGpa != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("gpa"), minGpa));
            }

            if (maxGpa != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("gpa"), maxGpa));
            }

            if (courseId != null) {
                spec = spec.and((root, query, cb) -> {
                    Join<User, Course> courses = root.join("courses");
                    return cb.equal(courses.get("id"), courseId);
                });
            }

            Page<User> userPage = userRepository.findAll(spec, pageable);
            List<UserResponse> students = userPage.getContent().stream()
                    .map(user -> new UserResponse(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getPhone(),
                            user.getGpa(),
                            user.getStatus(),
                            user.getCreatedAt().toString(),
                            user.getRole().name(),
                            user.getCourses().stream().map(Course::getName).collect(Collectors.toList())))
                    .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                    "students", students,
                    "currentPage", userPage.getNumber(),
                    "totalItems", userPage.getTotalElements(),
                    "totalPages", userPage.getTotalPages()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error fetching students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch students: " + e.getMessage()));
        }
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        logger.info("Processing request to view student details for ID: {}", id);
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User admin = userRepository.findByEmail(email);
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to view student details by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can view student details"));
            }

            User user = userRepository.findById(id).orElse(null);
            if (user == null || !user.getRole().equals(Role.STUDENT)) {
                logger.warn("Student not found or not a student for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND, "Student not found with ID: " + id));
            }

            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getGpa(),
                    user.getStatus(),
                    user.getCreatedAt().toString(),
                    user.getRole().name(),
                    user.getCourses().stream().map(Course::getName).collect(Collectors.toList())
            );

            logger.info("Successfully retrieved student details for ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error fetching student details for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch student details: " + e.getMessage()));
        }
    }
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User admin = userRepository.findByEmail(email);
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                logger.warn("Unauthorized attempt to view stats by email: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse(HttpStatus.FORBIDDEN, "Only ADMIN can view stats"));
            }
            long totalStudents = userRepository.countByRole(Role.STUDENT);
            long activeCourses = courseRepository.countByStatus("ACTIVE");
            double avgGpa = userRepository.findByRole(Role.STUDENT).stream()
                    .filter(u -> u.getGpa() != null)
                    .mapToDouble(User::getGpa)
                    .average()
                    .orElse(0.0);
            return ResponseEntity.ok(Map.of(
                    "totalStudents", totalStudents,
                    "activeCourses", activeCourses,
                    "avgPerformance", Math.round(avgGpa * 25.0)
            ));
        } catch (Exception e) {
            logger.error("Unexpected error fetching stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch stats: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            User user = userRepository.findByEmail(email);
            if (user == null) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND, "User not found"));
            }
            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getGpa(),
                    user.getStatus(),
                    user.getCreatedAt().toString(),
                    user.getRole().name(),
                    user.getCourses().stream().map(Course::getName).collect(Collectors.toList())
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch profile: " + e.getMessage()));
        }
    }
}

class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String password;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class AdminCreateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String phone;
    private Double gpa;
    private String status;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class UserResponse {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final Double gpa;
    private final String status;
    private final String createdAt;
    private final String role;
    private final List<String> courses;

    public UserResponse(Long id, String firstName, String lastName, String email, String phone, Double gpa, String status, String createdAt, String role, List<String> courses) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.gpa = gpa;
        this.status = status;
        this.createdAt = createdAt;
        this.role = role;
        this.courses = courses;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Double getGpa() { return gpa; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getRole() { return role; }
    public List<String> getCourses() { return courses; }
}

