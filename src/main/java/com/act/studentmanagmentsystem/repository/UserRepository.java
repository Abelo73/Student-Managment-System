package com.act.studentmanagmentsystem.repository;

import com.act.studentmanagmentsystem.entity.Role;
import com.act.studentmanagmentsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    Page<User> findAll(Specification<User> spec, Pageable pageable);
}