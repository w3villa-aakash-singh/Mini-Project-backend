package com.w3villa.mini_project_backend.services;

import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.entites.PlanType;
import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface UserService {

    // Existing methods
    UserDto createUser(UserDto userDto);
    UserDto getUserByEmail(String email);
    UserDto updateUser(UserDto userDto, String userId);
    void deleteUser(String userId);
    UserDto getUserById(String userId);
    Iterable<UserDto> getAllUsers();

    byte[] generateUserProfilePdf(String userId);

    // STEP 4: Email Verification Methods
    void register(UserDto userDto, String siteURL) throws MessagingException, UnsupportedEncodingException;

    boolean verify(String verificationCode);
    void upgradeUserPlan(String userId, PlanType newPlan);

    String uploadProfilePicture(String userId, MultipartFile file) throws IOException;
}