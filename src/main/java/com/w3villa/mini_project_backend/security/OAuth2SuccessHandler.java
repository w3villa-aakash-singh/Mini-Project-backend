package com.w3villa.mini_project_backend.security;

import com.w3villa.mini_project_backend.entites.*;
import com.w3villa.mini_project_backend.repositories.RefreshTokenRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import com.w3villa.mini_project_backend.repositories.RoleRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JWTService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontEndSuccessUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        logger.info("OAuth2 Authentication Success starting...");

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String registrationId = "unknown";
            if (authentication instanceof OAuth2AuthenticationToken token) {
                registrationId = token.getAuthorizedClientRegistrationId();
            }

            logger.info("Provider: {}", registrationId);

            User user;

            switch (registrationId) {

                // ================= GOOGLE =================
                case "google" -> {
                    String googleId = oAuth2User.getAttribute("sub");
                    String email = oAuth2User.getAttribute("email");
                    String name = oAuth2User.getAttribute("name");
                    String picture = oAuth2User.getAttribute("picture");

                    Optional<User> existingUserOpt = userRepository.findByEmail(email);

                    if (existingUserOpt.isPresent()) {
                        // ✅ MERGE
                        user = existingUserOpt.get();

                        user.setProvider(Provider.GOOGLE);
                        user.setProviderId(googleId);
                        user.setEnabled(true);
                        user.setName(name);
                        user.setImage(picture);

                    } else {
                        // ✅ CREATE NEW
                        user = User.builder()
                                .email(email)
                                .name(name)
                                .image(picture)
                                .enabled(true)
                                .provider(Provider.GOOGLE)
                                .providerId(googleId)
                                .build();
                    }
                }

                // ================= GITHUB =================
                case "github" -> {
                    String githubId = String.valueOf(oAuth2User.getAttribute("id"));
                    String name = oAuth2User.getAttribute("login");
                    String image = oAuth2User.getAttribute("avatar_url");
                    String email = oAuth2User.getAttribute("email");

                    if (email == null) {
                        email = name + "@github.com";
                    }

                    Optional<User> existingUserOpt = userRepository.findByEmail(email);

                    if (existingUserOpt.isPresent()) {
                        // ✅ MERGE
                        user = existingUserOpt.get();

                        user.setProvider(Provider.GITHUB);
                        user.setProviderId(githubId);
                        user.setEnabled(true);
                        user.setName(name);
                        user.setImage(image);

                    } else {
                        // ✅ CREATE NEW
                        user = User.builder()
                                .email(email)
                                .name(name)
                                .image(image)
                                .enabled(true)
                                .provider(Provider.GITHUB)
                                .providerId(githubId)
                                .build();
                    }
                }

                default -> throw new RuntimeException("Invalid provider: " + registrationId);
            }

            // ================= ROLE SAFETY =================
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                Role role = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
                user.setRoles(Set.of(role));
            }

            userRepository.save(user);

            // ================= JWT + REFRESH =================
            String jti = UUID.randomUUID().toString();

            RefreshToken refreshTokenOb = RefreshToken.builder()
                    .jti(jti)
                    .user(user)
                    .revoked(false)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                    .build();

            refreshTokenRepository.save(refreshTokenOb);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user, jti);

            // Cookie
            cookieService.attachRefreshCookie(
                    response,
                    refreshToken,
                    (int) jwtService.getRefreshTtlSeconds()
            );

            // ================= REDIRECT =================
            String finalRedirectBase = frontEndSuccessUrl.contains(",")
                    ? frontEndSuccessUrl.split(",")[0].trim()
                    : frontEndSuccessUrl;

            String targetUrl = UriComponentsBuilder
                    .fromUriString(finalRedirectBase)
                    .queryParam("token", accessToken)
                    .build()
                    .toUriString();

            logger.info("Redirecting to: {}", targetUrl);
            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            logger.error("OAuth Error: ", e);

            String fallbackBase = frontEndSuccessUrl.split(",")[0].trim();

            String errorUrl = UriComponentsBuilder
                    .fromUriString(fallbackBase)
                    .replacePath("/oauth/failure")
                    .queryParam("error", e.getMessage())
                    .build()
                    .toUriString();

            response.sendRedirect(errorUrl);
        }
    }
}