package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.dtos.*;
import com.w3villa.mini_project_backend.entites.RefreshToken;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.RefreshTokenRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import com.w3villa.mini_project_backend.security.CookieService;
import com.w3villa.mini_project_backend.security.JWTService;
import com.w3villa.mini_project_backend.services.AuthService;
import com.w3villa.mini_project_backend.services.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.mail.MessagingException;
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
import org.springframework.transaction.annotation.Transactional; // 🚩 Ensure this import
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Arrays;
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
    private final AuthService authService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;

    @PostMapping("/login")
    @Transactional // 🚩 Added to handle Lazy loading of roles during token generation
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authenticate = authenticate(loginRequest);

        // 🚩 Use the new FETCH JOIN method
        User user = userRepository.findByEmailWithRoles(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid Username or Password"));

        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Please verify your email before logging in.");
        }

        String jti = UUID.randomUUID().toString();
        var refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken,
                jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class));

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDto userDto, HttpServletRequest request)
            throws MessagingException, UnsupportedEncodingException {

        String siteURL = request.getRequestURL().toString().replace(request.getServletPath(), "");
        String authBaseUrl = siteURL + "/api/v1/auth";

        userService.register(userDto, authBaseUrl);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Registration successful! Please check your email to verify your account.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam("code") String code) {
        if (userService.verify(code)) {
            return ResponseEntity.ok("Account verified successfully! You can now login.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Verification failed: The code is invalid or has already been used.");
        }
    }

    @PostMapping("/refresh")
    @Transactional // 🚩 CRITICAL: Keeps session open for user.getRoles() in JWTService
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String refreshToken = readRefreshTokenFromRequest(body, request)
                .orElseThrow(() -> new BadCredentialsException("Refresh token is missing"));

        if(!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not recognized"));

        if(storedRefreshToken.isRevoked() || storedRefreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        // 🚩 Re-fetch user with Roles to avoid LazyInitializationException
        User user = userRepository.findByIdWithRoles(storedRefreshToken.getUser().getId())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        storedRefreshToken.setRevoked(true);
        refreshTokenRepository.save(storedRefreshToken);

        String newJti = UUID.randomUUID().toString();
        var newRefreshTokenOb = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshTokenOb);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken,
                jwtService.getAccessTtlSeconds(), mapper.map(user, UserDto.class)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
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

    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> !v.isBlank())
                    .findFirst();
            if (fromCookie.isPresent()) return fromCookie;
        }
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }
        return Optional.empty();
    }

    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid Username or Password !!");
        }
    }
}