package com.w3villa.mini_project_backend.dtos;

public record LoginRequest(
        String email,
        String password
) {
}
