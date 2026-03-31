package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Added for Email Verification
    User findByVerificationCode(String verificationCode);
    List<User> findByPlanExpiryBeforeAndPlanTypeNot(Instant expiry, PlanType planType);
}