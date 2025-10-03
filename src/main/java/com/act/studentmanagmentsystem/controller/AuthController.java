package com.act.studentmanagmentsystem.controller;

import com.act.studentmanagmentsystem.entity.Role;
import com.act.studentmanagmentsystem.entity.User;
import com.act.studentmanagmentsystem.repository.UserRepository;
import com.act.studentmanagmentsystem.service.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        logger.info("Processing registration request for email: {}", request.getEmail());
        try {
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
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(role);
            user.setPhone(request.getPhone());
            user.setGpa(request.getGpa());
            user.setStatus(request.getStatus());
            user.setCreatedAt(java.time.LocalDateTime.now());

            userRepository.save(user);
            logger.info("User registered successfully: {}", request.getEmail());
            return ResponseEntity.ok("User registered successfully");
        } catch (DataIntegrityViolationException e) {
            logger.warn("Registration failed: Email already exists: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT, "Email already exists: " + request.getEmail()));
        } catch (Exception e) {
            logger.error("Unexpected error during registration for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        logger.info("Processing login request for email: {}", request.getEmail());
        try {
            User user = userRepository.findByEmail(request.getEmail());
            if (user == null) {
                logger.warn("Login failed: User not found for email: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid email or password"));
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.warn("Login failed: Incorrect password for email: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, "Invalid email or password"));
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
            logger.info("Login successful for email: {}, token generated", request.getEmail());
            return ResponseEntity.ok(new LoginResponse(true, token, user.isMustChangePassword()));
        } catch (Exception e) {
            logger.error("Unexpected error during login for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        logger.info("Processing logout request");
        return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
    }
}

class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class LoginRequest {
    private String email;
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class LoginResponse {
    private final boolean status;
    private final String token;
    private final boolean mustChangePassword;

    public LoginResponse(boolean status, String token, boolean mustChangePassword) {
        this.status = status;
        this.token = token;
        this.mustChangePassword = mustChangePassword;
    }

    public boolean isStatus() { return status; }
    public String getToken() { return token; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}

class LogoutResponse {
    private final boolean status;
    private final String message;

    public LogoutResponse(boolean status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isStatus() { return status; }
    public String getMessage() { return message; }
}

class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;

    public ErrorResponse(HttpStatus status, String message) {
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
}