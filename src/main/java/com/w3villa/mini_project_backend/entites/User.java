package com.w3villa.mini_project_backend.entites;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.Instant;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="user_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name= "user_email", unique = true, length = 300, nullable = false)
    private String email;

    @Column(name= "user_name", length = 500)
    private String name;

    private String password;
    private String image;

    @Column(name = "formatted_address", length = 1000)
    private String formattedAddress;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(name = "verification_code", length = 64)
    private String verificationCode;

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Provider provider = Provider.LOCAL;

    private String providerId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type")
    private PlanType planType = PlanType.FREE;

    @Column(name = "plan_expiry")
    private Instant planExpiry;

    @Column(name = "plan_activated_at")
    private Instant planActivatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override public String getUsername() { return this.email; }
    @Override public boolean isEnabled() { return this.enabled; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}