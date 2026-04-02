package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.RefreshToken;
import com.w3villa.mini_project_backend.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(String jti);

        void deleteByUser(User user); // 🔥 ADD THIS

}
