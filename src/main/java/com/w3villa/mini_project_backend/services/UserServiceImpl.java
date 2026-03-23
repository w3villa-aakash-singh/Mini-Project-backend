package com.w3villa.mini_project_backend.services;

import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.entites.Provider;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.exceptions.ResourceNotFoundException;
import com.w3villa.mini_project_backend.helpers.UserHelper;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if(userDto.getEmail()==null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already Exits");

        }
        User user = modelMapper.map(userDto,User.class);
        user.setProvider(userDto.getProvider()!=null ? userDto.getProvider() : Provider.LOCAL);
        User savedUser =userRepository.save(user);
        return modelMapper.map(savedUser,UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {
       User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with given email Id"));

        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User existingUser = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));
        if (userDto.getName()!=null)existingUser.setName(userDto.getName());
        if (userDto.getImage()!=null)existingUser.setEmail(userDto.getEmail());
        if (userDto.getProvider()!=null)existingUser.setProvider(userDto.getProvider());
        if (userDto.getPassword()!=null)existingUser.setPassword(userDto.getPassword());
        existingUser.setEnable(userDto.isEnable());
        existingUser.setUpdateAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser,UserDto.class);


    }

    @Override
    public void deleteUser(String userId) {
        UUID uId  = UserHelper.parseUUID(userId);
       User user = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));
       userRepository.delete(user);

    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId)).orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(user -> modelMapper.map(user, UserDto.class)).toList();
    }
}
