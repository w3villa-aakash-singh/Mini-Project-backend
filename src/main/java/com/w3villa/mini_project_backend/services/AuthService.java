package com.w3villa.mini_project_backend.services;

import com.w3villa.mini_project_backend.dtos.UserDto;

public interface AuthService {
    UserDto registerUser(UserDto userDto);
}
