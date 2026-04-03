package com.w3villa.mini_project_backend.security;

import com.w3villa.mini_project_backend.entites.Provider;
import com.w3villa.mini_project_backend.entites.RefreshToken;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.RefreshTokenRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontEndSuccessUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        System.out.println("🚩 [DEBUG] Entering onAuthenticationSuccess...");
        logger.info("🚩 OAuth2 Authentication Success starting...");

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            // Identify Registration Provider
            String registrationId = "unknown";
            if (authentication instanceof OAuth2AuthenticationToken token) {
                registrationId = token.getAuthorizedClientRegistrationId();
            }

            logger.info("🚩 Registration Provider: {}", registrationId);
            logger.info("🚩 User Attributes: {}", oAuth2User.getAttributes());

            User user;
            switch (registrationId) {
                case "google" -> {
                    String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();
                    String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                    String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                    String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();

                    logger.info("🚩 Processing Google User: {}", email);

                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .image(picture)
                            .enabled(true)
                            .provider(Provider.GOOGLE)
                            .providerId(googleId)
                            .build();

                    user = userRepository.findByEmail(email).orElseGet(() -> {
                        logger.info("🚩 Creating new Google user in DB...");
                        return userRepository.save(newUser);
                    });
                }

                case "github" -> {
                    String name = oAuth2User.getAttributes().getOrDefault("login", "").toString();
                    String githubId = oAuth2User.getAttributes().getOrDefault("id", "").toString();
                    String image = oAuth2User.getAttributes().getOrDefault("avatar_url", "").toString();
                    String email = (String) oAuth2User.getAttributes().get("email");

                    if (email == null) {
                        email = name + "@github.com";
                    }

                    logger.info("🚩 Processing GitHub User: {}", email);

                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .image(image)
                            .enabled(true)
                            .provider(Provider.GITHUB)
                            .providerId(githubId)
                            .build();

                    user = userRepository.findByEmail(email).orElseGet(() -> {
                        logger.info("🚩 Creating new GitHub user in DB...");
                        return userRepository.save(newUser);
                    });
                }

                default -> {
                    logger.error("❌ Invalid registration ID: {}", registrationId);
                    throw new RuntimeException("Invalid registration id: " + registrationId);
                }
            }

            // Create Refresh Token Object
            logger.info("🚩 Generating Refresh Token for JTI...");
            String jti = UUID.randomUUID().toString();
            RefreshToken refreshTokenOb = RefreshToken.builder()
                    .jti(jti)
                    .user(user)
                    .revoked(false)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                    .build();

            refreshTokenRepository.save(refreshTokenOb);
            logger.info("🚩 Refresh Token saved to DB. JTI: {}", jti);

            // Generate JWTs
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

            // Attach Refresh Token to Cookie
            logger.info("🚩 Attaching Refresh Token to secure cookie...");
            cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());

            // 🚩 THE MULTI-URL FIX: Take only the first URL if comma-separated
            String finalRedirectBase = frontEndSuccessUrl;
            if (finalRedirectBase.contains(",")) {
                logger.warn("🚩 Multiple redirect URLs detected. Picking the first one...");
                finalRedirectBase = finalRedirectBase.split(",")[0].trim();
            }

            // Construct Final Redirect URL with Access Token
            String targetUrl = UriComponentsBuilder.fromUriString(finalRedirectBase)
                    .queryParam("token", accessToken)
                    .build().toUriString();

            logger.info("🚩 Redirecting to Frontend Success Page: {}", targetUrl);
            System.out.println("🚩 [DEBUG] Redirecting to: " + targetUrl);

            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            logger.error("❌ CRITICAL ERROR in OAuth2SuccessHandler: ", e);
            System.out.println("❌ [DEBUG] Error: " + e.getMessage());

            // If redirection base is broken, use a safe default or extract from broken string
            String fallbackBase = frontEndSuccessUrl.split(",")[0].trim();
            String errorUrl = UriComponentsBuilder.fromUriString(fallbackBase)
                    .replacePath("/oauth/failure")
                    .queryParam("error", e.getMessage())
                    .build().toUriString();
            response.sendRedirect(errorUrl);
        }
    }
}