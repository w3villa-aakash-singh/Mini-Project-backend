package com.w3villa.mini_project_backend.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name="roles")
public class Role {
    @Id
    @Column(name="role_id")
    private UUID id= UUID.randomUUID();
    @Column(unique = true,nullable = false)
    private String name;
}
