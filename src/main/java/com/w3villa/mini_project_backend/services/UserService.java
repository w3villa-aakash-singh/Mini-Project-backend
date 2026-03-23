package com.w3villa.mini_project_backend.services;

import com.w3villa.mini_project_backend.dtos.UserDto;

public interface UserService {

    UserDto createUser(UserDto userDto);

    UserDto getUserByEmail(String email);

    UserDto updateUser(UserDto userDto ,String userId);

    void  deleteUser(String userId);

    UserDto getUserById(String userId);

    Iterable<UserDto> getAllUsers();

}
