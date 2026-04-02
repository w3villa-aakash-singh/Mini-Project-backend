package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.services.UserService;

import lombok.AllArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    // ---------------- PUBLIC / AUTH ----------------

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(userDto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto,
                                              @PathVariable String userId){
        return ResponseEntity.ok(userService.updateUser(userDto, userId));
    }

    // ---------------- ADMIN ONLY ----------------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId){
        userService.deleteUser(userId);
        return ResponseEntity.ok("User deleted successfully!");
    }

    // 🔥 NEW: Upgrade plan (ADMIN FEATURE)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/upgrade")
    public ResponseEntity<String> upgradeUserPlan(
            @PathVariable String userId,
            @RequestParam PlanType planType){

        userService.upgradeUserPlan(userId, planType);
        return ResponseEntity.ok("User upgraded to " + planType);
    }

    // ---------------- FILE / PDF ----------------

    @GetMapping("/{userId}/download")
    public ResponseEntity<byte[]> downloadUserProfile(@PathVariable String userId) {
        byte[] pdfData = userService.generateUserProfilePdf(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=User_" + userId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @PostMapping("/{userId}/upload-image")
    public ResponseEntity<String> uploadProfileImage(
            @PathVariable String userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String imageUrl = userService.uploadProfilePicture(userId, file);
        return ResponseEntity.ok(imageUrl);
    }
}