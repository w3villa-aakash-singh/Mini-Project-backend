package com.w3villa.mini_project_backend.services.impl;

// CORRECT PDF IMPORTS
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.entites.Provider;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.exceptions.ResourceNotFoundException;
import com.w3villa.mini_project_backend.helpers.UserHelper;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import com.w3villa.mini_project_backend.services.FileService;
import com.w3villa.mini_project_backend.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {



    @Value("${app.auth.frontend.base-url}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final FileService fileService;

    // --- STEP 4: REGISTRATION WITH EMAIL VERIFICATION ---

    @Override
    @Transactional
    public void register(UserDto userDto, String siteURL)
            throws MessagingException, UnsupportedEncodingException {

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = modelMapper.map(userDto, User.class);

        // Encode password and set defaults
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);

        // Generate Verification Code
        String randomCode = UUID.randomUUID().toString();
        user.setVerificationCode(randomCode);
        user.setEnabled(false); // Account locked until verified

        userRepository.save(user);

        // Trigger Email Send
        sendVerificationEmail(user, siteURL);
    }
    private void sendVerificationEmail(User user, String siteURL)
            throws MessagingException, UnsupportedEncodingException {

        String toAddress = user.getEmail();
        String fromAddress = "your_email@gmail.com"; // Consider moving this to application.yml too!
        String senderName = "W3Villa Project";
        String subject = "Please verify your registration";

        String content = "Dear [[name]],<br>"
                + "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY ACCOUNT</a></h3>"
                + "Thank you,<br>"
                + "W3Villa Team.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getName());

        // --- UPDATED LOGIC ---
        // This now points to: http://localhost:5173/verify?code=...
        String verifyURL = frontendBaseUrl + "/verify?code=" + user.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);
        mailSender.send(message);
    }

    @Override
    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);

        if (user == null || user.isEnabled()) {
            return false;
        } else {
            user.setVerificationCode(null); // Clear the code
            user.setEnabled(true);          // Unlock account
            userRepository.save(user);
            return true;
        }
    }

    // --- EXISTING CRUD METHODS ---

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if(userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }
        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given email Id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User existingUser = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));

        // Standard profile updates
        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if (userDto.getEmail() != null) existingUser.setEmail(userDto.getEmail());

        // --- NEW: ADDRESS & MAP UPDATES ---
        if (userDto.getFormattedAddress() != null) existingUser.setFormattedAddress(userDto.getFormattedAddress());
        if (userDto.getLatitude() != null) existingUser.setLatitude(userDto.getLatitude());
        if (userDto.getLongitude() != null) existingUser.setLongitude(userDto.getLongitude());
        // ----------------------------------

        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        existingUser.setEnabled(userDto.isEnabled());
        existingUser.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));
        userRepository.delete(user);
    }
    @Override
    public byte[] generateUserProfilePdf(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // 1. Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Color.RED);
            Paragraph title = new Paragraph("USER IDENTITY", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);

            // 2. Data Section
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            addPdfLine(document, "IDENTITY_ID: ", user.getId().toString(), labelFont, dataFont);
            addPdfLine(document, "FULL_NAME: ", user.getName(), labelFont, dataFont);
            addPdfLine(document, "EMAIL: ", user.getEmail(), labelFont, dataFont);
            addPdfLine(document, "VERIFICATION: ", user.isEnabled() ? "SECURED" : "UNVERIFIED", labelFont, dataFont);

            document.add(new Paragraph(" ")); // Spacer

            // 3. Location Section
            Paragraph locHeader = new Paragraph("GEOSPATIAL COORDINATES", labelFont);
            locHeader.setSpacingBefore(10);
            document.add(locHeader);

            addPdfLine(document, "ADDRESS: ", user.getFormattedAddress(), labelFont, dataFont);

            String coords = (user.getLatitude() != null) ? user.getLatitude() + ", " + user.getLongitude() : "N/A";
            addPdfLine(document, "COORDINATES: ", coords, labelFont, dataFont);

            document.add(new Paragraph("\n--- END OF TRANSMISSION ---", dataFont));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF Generation Protocol Failed", e);
        }
    }
    @Override
    @Transactional
    public void upgradeUserPlan(String userId, PlanType newPlan) {
        System.out.println("DEBUG: Webhook Triggered Upgrade for ID: " + userId);

        UUID uId = UUID.fromString(userId);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Update Plan Details
        user.setPlanType(newPlan);
        user.setPlanActivatedAt(Instant.now());

        if (newPlan == PlanType.GOLD) {
            user.setPlanExpiry(Instant.now().plus(java.time.Duration.ofHours(12)));
        } else if (newPlan == PlanType.SILVER) {
            user.setPlanExpiry(Instant.now().plus(java.time.Duration.ofHours(6)));
        } else {
            user.setPlanExpiry(null);
        }

        // saveAndFlush forces Hibernate to send the SQL UPDATE command to MySQL immediately
        userRepository.saveAndFlush(user);

        System.out.println("✅ DATABASE PERSISTED: User " + user.getEmail() + " is now " + user.getPlanType());
    }

    private void addPdfLine(Document doc, String label, String value, Font lFont, Font dFont) throws Exception {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, lFont));
        p.add(new Chunk(value != null ? value : "NOT_FOUND", dFont));
        doc.add(p);
    }
    // Helper method to keep the PDF code clean
    private void addKeyPair(Document doc, String label, String value, Font lFont, Font dFont) throws Exception {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(label, lFont));
        p.add(new com.lowagie.text.Chunk(value != null ? value : "N/A", dFont));
        doc.add(p);
    }
    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
    @Override
    @Transactional
    public String uploadProfilePicture(String userId, MultipartFile file) throws IOException {
        // 1. Find the existing user using your UserHelper
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found With Given Id"));

        // 2. Create a unique path/name for Storj (e.g., profiles/UUID_1711812345.png)
        String originalName = file.getOriginalFilename();
        String extension = originalName != null ? originalName.substring(originalName.lastIndexOf(".")) : ".png";
        String fileName = "profiles/" + user.getId() + "_" + System.currentTimeMillis() + extension;

        // 3. Use the FileService to push bytes to Storj
        // This returns the public URL: https://gateway.storjshare.io/user-profile/profiles/...
        String imageUrl = fileService.uploadImage(file, fileName);

        // 4. Save that URL to the 'image' column in your MySQL 'users' table
        user.setImage(imageUrl);
        userRepository.save(user);

        // 5. Return the URL so the frontend can update the Avatar immediately
        return imageUrl;
    }
}