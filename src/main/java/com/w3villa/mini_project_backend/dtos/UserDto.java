package com.w3villa.mini_project_backend.dtos;

import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.entites.Provider;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String email;
    private String name;
    private String password;
    private String image;
    private boolean enabled;

    // --- NEW ADDRESS FIELDS ---
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    // --------------------------

    private Instant createdAt;
    private Instant updatedAt;

    private Provider provider;
    // Inside UserDto.java
    private PlanType planType;
    private java.time.Instant planExpiry;
    private java.time.Instant planActivatedAt;

    private Set<RoleDto> roles = new HashSet<>();
}