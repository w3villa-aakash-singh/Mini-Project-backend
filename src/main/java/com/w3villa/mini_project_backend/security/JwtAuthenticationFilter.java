package com.w3villa.mini_project_backend.security;
import com.w3villa.mini_project_backend.helpers.UserHelper;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        logger.info("Authorization header : {}", header);

        if (header != null && header.startsWith("Bearer ")) {


            //token extract and validate then authentication create and then security context ke ander set karunga.

            String token = header.substring(7);
            //check for access token


            try {
                if (!jwtService.isAccessToken(token)) {
                    //message pass kar hai---
                    filterChain.doFilter(request, response);
                    return;
                }




                Jws<Claims> parse = jwtService.parse(token);


                Claims payload = parse.getPayload();


                String userId = payload.getSubject();
                UUID userUuid = UserHelper.parseUUID(userId);

                userRepository.findById(userUuid).ifPresent(user -> {

                    //check for user enable or not

                    if (user.isEnabled()) {
                        // user mil chuka hai database se
                        List<GrantedAuthority> authorities = user.getRoles() == null ? List.of() : user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        //final line : to set the authentication to security context
                        if (SecurityContextHolder.getContext().getAuthentication() == null)
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                    }


                });


            } catch (ExpiredJwtException e) {
//                request.setAttribute("error", "Token Expired");
                 e.printStackTrace();

            } catch (Exception e) {
//                request.setAttribute("error", "Invalid Token");
                e.printStackTrace();

            }


        }

        filterChain.doFilter(request, response);


    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Add the upload-image path to the skip list
        return path.startsWith("/api/v1/auth") ||
                path.contains("/upload-image") ||
                path.contains("/download");
    }
}
