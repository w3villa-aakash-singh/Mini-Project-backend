package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.dtos.*;
import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.entites.RefreshToken;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.RefreshTokenRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import com.w3villa.mini_project_backend.security.CookieService;
import com.w3villa.mini_project_backend.security.JWTService;
import com.w3villa.mini_project_backend.services.UserService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final ModelMapper mapper;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;

    // =========================
    // LOGIN
    // =========================
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletResponse response) {

        authenticate(loginRequest);

        User user = userRepository.findByEmailWithRoles(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid Username or Password"));

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Please verify your email before logging in.");
        }

        String jti = UUID.randomUUID().toString();

        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        cookieService.attachRefreshCookie(response, refreshToken,
                (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        TokenResponse tokenResponse = TokenResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTtlSeconds(),
                mapper.map(user, UserDto.class)
        );

        return ResponseEntity.ok(tokenResponse);
    }

    // =========================
    // REGISTER
    // =========================
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDto userDto,
                                               HttpServletRequest request) {
        try {
            String siteURL = request.getRequestURL().toString()
                    .replace(request.getServletPath(), "");

            String authBaseUrl = siteURL + "/api/v1/auth";

            userService.register(userDto, authBaseUrl);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Registration successful! Please check your email.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Registration failed. Try again later.");
        }
    }

    // =========================
    // VERIFY EMAIL
    // =========================
    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("code") String code) {
        if (userService.verify(code)) {
            return ResponseEntity.ok("Account verified successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid or expired verification code.");
        }
    }

    // =========================
    // REFRESH TOKEN
    // =========================
    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request) {

        String refreshToken = readRefreshTokenFromRequest(body, request)
                .orElseThrow(() -> new BadCredentialsException("Refresh token missing"));

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String jti = jwtService.getJti(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Token not found"));

        if (storedToken.isRevoked() ||
                storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Token expired or revoked");
        }

        User user = userRepository.findByIdWithRoles(storedToken.getUser().getId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // revoke old
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // create new
        String newJti = UUID.randomUUID().toString();

        RefreshToken newToken = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newJti);

        cookieService.attachRefreshCookie(response, newRefreshToken,
                (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTtlSeconds(),
                mapper.map(user, UserDto.class)
        ));
    }

    // =========================
    // LOGOUT
    // =========================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {

        readRefreshTokenFromRequest(null, request).ifPresent(token -> {
            try {
                if (jwtService.isRefreshToken(token)) {
                    String jti = jwtService.getJti(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            } catch (JwtException ignored) {}
        });

        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // =========================
    // CURRENT USER
    // =========================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized");
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean expired = false;

        // 🔥 CHECK EXPIRY HERE (REAL-TIME FIX)
        if (user.getPlanExpiry() != null &&
                user.getPlanExpiry().isBefore(Instant.now())) {

            user.setPlanType(PlanType.FREE);
            user.setPlanExpiry(null);
            userRepository.save(user);

            expired = true;
        }

        UserDto userDto = mapper.map(user, UserDto.class);

        // 🔥 RETURN EXTRA FLAG
        return ResponseEntity.ok(
                Map.of(
                        "user", userDto,
                        "expired", expired
                )
        );
    }
    // =========================
    // HELPERS
    // =========================
    private Optional<String> readRefreshTokenFromRequest(
            RefreshTokenRequest body,
            HttpServletRequest request) {

        if (request.getCookies() != null) {
            Optional<String> cookieToken = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> !v.isBlank())
                    .findFirst();

            if (cookieToken.isPresent()) return cookieToken;
        }

        if (body != null && body.refreshToken() != null &&
                !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }

        return Optional.empty();
    }

    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid Username or Password");
        }
    }
}