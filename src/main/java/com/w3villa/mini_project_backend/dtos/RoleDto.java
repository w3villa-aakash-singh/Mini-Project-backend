package com.w3villa.mini_project_backend.dtos;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleDto {

    private UUID id= UUID.randomUUID();
    private String name;
}
