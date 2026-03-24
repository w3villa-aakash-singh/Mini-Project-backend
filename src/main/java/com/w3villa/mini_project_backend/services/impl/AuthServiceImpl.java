package com.w3villa.mini_project_backend.services.impl;

import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.services.AuthService;
import com.w3villa.mini_project_backend.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto registerUser(UserDto userDto) {

        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userService.createUser(userDto);
    }
}
