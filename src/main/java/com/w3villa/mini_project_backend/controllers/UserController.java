package com.w3villa.mini_project_backend.controllers;


import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }

    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email){
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId){
        return ResponseEntity.ok(userService.getUserById(userId));
    }



    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@RequestBody UserDto userDto, @PathVariable String userId){
        // This now handles the address, lat, and lng automatically via the updated service
        return ResponseEntity.ok(userService.updateUser(userDto, userId));

    }

    @GetMapping("/{userId}/download")
    public ResponseEntity<byte[]> downloadUserProfile(@PathVariable String userId) {
        byte[] pdfData = userService.generateUserProfilePdf(userId);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Dossier_" + userId + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfData);
    }


    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId){
        userService.deleteUser(userId);
        return ResponseEntity.ok("User deleted successfully!");
    }

    /**
     * Requirement 4.1: Handle Profile Picture Upload to Storj
     */
    @PostMapping("/{userId}/upload-image")
    public ResponseEntity<String> uploadProfileImage(
            @PathVariable String userId,
            @RequestParam("file") MultipartFile file) throws IOException {

        // This calls the specific Storj upload logic in your service
        String imageUrl = userService.uploadProfilePicture(userId, file);
        return ResponseEntity.ok(imageUrl);
    }
}