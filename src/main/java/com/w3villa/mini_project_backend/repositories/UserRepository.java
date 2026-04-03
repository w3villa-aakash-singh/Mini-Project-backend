package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User findByVerificationCode(String verificationCode);

    List<User> findByPlanExpiryBeforeAndPlanTypeNot(Instant expiry, PlanType planType);
}