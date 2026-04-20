package com.w3villa.mini_project_backend.services.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import java.util.List;

import com.sendgrid.Method;
import com.w3villa.mini_project_backend.dtos.RoleDto;
import com.w3villa.mini_project_backend.dtos.UserDto;
import com.w3villa.mini_project_backend.entites.*;
import com.w3villa.mini_project_backend.exceptions.ResourceNotFoundException;
import com.w3villa.mini_project_backend.helpers.UserHelper;
import com.w3villa.mini_project_backend.repositories.CartRepository;
import com.w3villa.mini_project_backend.repositories.OrderRepository;
import com.w3villa.mini_project_backend.repositories.RefreshTokenRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import com.w3villa.mini_project_backend.services.FileService;
import com.w3villa.mini_project_backend.services.UserService;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${app.auth.frontend.base-url}")
    private String frontendBaseUrl;


    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${SENDGRID_SENDER_EMAIL}")
    private String senderEmail;

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final  RefreshTokenRepository refreshTokenRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    // ✅ CENTRAL ROLE MAPPING METHOD
    private UserDto mapToDto(User user) {
        UserDto dto = modelMapper.map(user, UserDto.class);

        dto.setRoles(
                user.getRoles().stream()
                        .map(role -> RoleDto.builder()
                                .id(role.getId())
                                .name(role.getName())
                                .build())
                        .collect(Collectors.toSet())
        );

        return dto;
    }

    // ---------------- REGISTER ----------------
    @Override
    @Transactional
    public void register(UserDto userDto, String siteURL)
            throws MessagingException, UnsupportedEncodingException {

        // 🚩 STEP 1: Check if user already exists
        java.util.Optional<User> existingUser = userRepository.findByEmail(userDto.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.isEnabled()) {
                // 🚩 SCENARIO A: User is already verified and active
                throw new IllegalArgumentException("Identity already confirmed. Please log in to the portal.");
            } else {
                // 🚩 SCENARIO B: User exists but is NOT verified. Update code and resend email.
                String newCode = UUID.randomUUID().toString();
                user.setVerificationCode(newCode);
                userRepository.save(user);

                sendVerificationEmail(user, siteURL);
                throw new IllegalArgumentException("Verification pending. A new security link has been dispatched to your inbox.");
            }
        }

        // 🚩 SCENARIO C: Brand new user registration
        User user = modelMapper.map(userDto, User.class);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);

        String randomCode = UUID.randomUUID().toString();
        user.setVerificationCode(randomCode);
        user.setEnabled(false);

        userRepository.save(user);
        sendVerificationEmail(user, siteURL);
    }

    private void sendVerificationEmail(User user, String siteURL) {
        String cleanBaseUrl = frontendBaseUrl;
        if (cleanBaseUrl.contains(",")) {
            cleanBaseUrl = cleanBaseUrl.split(",")[0].trim();
        }

        String verifyLink = cleanBaseUrl + "/verify?code=" + user.getVerificationCode();

        // SendGrid Setup
        Email from = new Email(senderEmail);
        String subject = "[PlanVerify] Identity Verification Required";
        Email to = new Email(user.getEmail());
        Content content = new Content("text/html",
                "<h3>PLANVERIFY SECURITY PROTOCOL</h3>" +
                        "<p>Click the link to verify your identity:</p>" +
                        "<a href=\"" + verifyLink + "\" style='background:blue; color:white; padding:10px; text-decoration:none;'>VERIFY IDENTITY</a>"
        );

        Mail mail = new Mail(from, subject, to, content);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            // Check for success (202 is the standard SendGrid success code)
            if (response.getStatusCode() >= 400) {
                System.err.println("SendGrid Error: " + response.getBody());
            }
        } catch (IOException ex) {
            System.err.println("SendGrid Exception: " + ex.getMessage());
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Override
    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);

        if (user == null || user.isEnabled()) return false;

        user.setVerificationCode(null);
        user.setEnabled(true);
        userRepository.save(user);
        return true;
    }

    // ---------------- CRUD ----------------

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        User user = modelMapper.map(userDto, User.class);
        User saved = userRepository.save(user);
        return mapToDto(saved); // ✅ FIX
    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDto(user); // ✅ FIX
    }

    // Add this helper method inside UserServiceImpl.java
    private User applyPlanCleanup(User user) {
        if (user.getPlanType() != PlanType.FREE &&
                user.getPlanExpiry() != null &&
                user.getPlanExpiry().isBefore(Instant.now())) {

            user.setPlanType(PlanType.FREE);
            user.setPlanExpiry(null);
            // Save the change so the DB is updated immediately
            return userRepository.save(user);
        }
        return user;
    }

    // Then update your fetch method:
    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check and update plan status BEFORE returning the DTO
        user = applyPlanCleanup(user);

        return mapToDto(user);
    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto) // ✅ FIX
                .toList();
    }

    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID id = UserHelper.parseUUID(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userDto.getName() != null) user.setName(userDto.getName());
        if (userDto.getEmail() != null) user.setEmail(userDto.getEmail());
        if (userDto.getImage() != null) user.setImage(userDto.getImage());

        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        if (userDto.getFormattedAddress() != null) {
            user.setFormattedAddress(userDto.getFormattedAddress());
        }

        if (userDto.getLatitude() != null) {
            user.setLatitude(userDto.getLatitude());
        }

        if (userDto.getLongitude() != null) {
            user.setLongitude(userDto.getLongitude());
        }


        user.setUpdatedAt(Instant.now());

        User updated = userRepository.save(user);
        return mapToDto(updated); // ✅ FIX
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {

        UUID uId = UserHelper.parseUUID(userId);

        User user = userRepository.findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        // 🔥 STEP 1: delete refresh tokens

        refreshTokenRepository.deleteByUser(user);

        // 🔥 STEP 2: clear roles
        user.getRoles().clear();
        userRepository.save(user);

        // 🔥 STEP 3: delete user
        userRepository.delete(user);
    }

    // ---------------- PLAN ----------------

    @Transactional
    public void upgradeUserPlan(String userId, PlanType newPlan) {

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PlanType currentPlan = user.getPlanType();

// 🔥 FIX: handle null plan
        if (currentPlan == null) {
            currentPlan = PlanType.FREE;
        }

// Prevent downgrade
        if (newPlan.ordinal() <= currentPlan.ordinal()) {
            System.out.println("Downgrade blocked for user: " + user.getEmail());
            return;
        }

        user.setPlanType(newPlan);
        user.setPlanActivatedAt(Instant.now());

        // set expiry
        if (newPlan == PlanType.GOLD) {
            user.setPlanExpiry(Instant.now().plus(Duration.ofHours(12)));
        } else if (newPlan == PlanType.SILVER) {
            user.setPlanExpiry(Instant.now().plus(Duration.ofHours(6)));
        }

        userRepository.save(user);
    }

    // ---------------- FILE UPLOAD ----------------

    @Override
    @Transactional
    public String uploadProfilePicture(String userId, MultipartFile file) throws IOException {

        User user = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String fileName = "profiles/" + user.getId() + "_" + System.currentTimeMillis();

        String imageUrl = fileService.uploadImage(file, fileName);

        user.setImage(imageUrl);
        userRepository.save(user);

        return imageUrl;
    }

    // ---------------- PDF ----------------

    @Override
    public byte[] generateUserProfilePdf(String userId) {

        User user = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 🔥 FETCH RELATED DATA
        List<Order> orders = orderRepository.findByUser(user);
        List<CartItem> cartItems = cartRepository.findByUser(user);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();

            // 🔷 FONTS
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLUE);
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.DARK_GRAY);
            Font normalFont = new Font(Font.HELVETICA, 12);
            Font footerFont = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);

            // 🔷 TITLE
            Paragraph title = new Paragraph("USER PROFILE DOSSIER", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            doc.add(new Paragraph(" "));

            // =========================
            // 👤 PERSONAL INFO
            // =========================
            doc.add(new Paragraph("Personal Information", headerFont));
            doc.add(new Paragraph("-----------------------------------"));

            doc.add(new Paragraph("Name: " + user.getName(), normalFont));
            doc.add(new Paragraph("Email: " + user.getEmail(), normalFont));
            doc.add(new Paragraph("User ID: " + user.getId(), normalFont));
            doc.add(new Paragraph("Provider: " + user.getProvider(), normalFont));

            doc.add(new Paragraph(" "));

            // =========================
            // ⚙️ ACCOUNT DETAILS
            // =========================
            doc.add(new Paragraph("Account Details", headerFont));
            doc.add(new Paragraph("-----------------------------------"));

            doc.add(new Paragraph("Created At: " + user.getCreatedAt(), normalFont));
            doc.add(new Paragraph("Updated At: " + user.getUpdatedAt(), normalFont));
            doc.add(new Paragraph("Enabled: " + user.isEnabled(), normalFont));

            doc.add(new Paragraph(" "));

            // =========================
            // 💎 PLAN DETAILS
            // =========================
            doc.add(new Paragraph("Plan Details", headerFont));
            doc.add(new Paragraph("-----------------------------------"));

            doc.add(new Paragraph("Plan Type: " + user.getPlanType(), normalFont));
            doc.add(new Paragraph("Activated At: " + user.getPlanActivatedAt(), normalFont));
            doc.add(new Paragraph("Expiry: " + user.getPlanExpiry(), normalFont));

            doc.add(new Paragraph(" "));

            // =========================
            // 📦 ORDERS
            // =========================
            doc.add(new Paragraph("Orders", headerFont));
            doc.add(new Paragraph("-----------------------------------"));

            if (orders.isEmpty()) {
                doc.add(new Paragraph("No orders found", normalFont));
            } else {
                for (Order order : orders) {

                    doc.add(new Paragraph("Order ID: " + order.getId(), normalFont));
                    doc.add(new Paragraph("Product: " + order.getProductName(), normalFont));
                    doc.add(new Paragraph("Price: $" + order.getProductPrice(), normalFont));
                    doc.add(new Paragraph("Quantity: " + order.getQuantity(), normalFont));
                    doc.add(new Paragraph("Paid: $" + order.getPaidAmount(), normalFont));
                    doc.add(new Paragraph("Status: " + order.getStatus(), normalFont));
                    doc.add(new Paragraph("Purchase Date: " + order.getPurchaseDate(), normalFont));

                    doc.add(new Paragraph("---------------"));
                }
            }

            doc.add(new Paragraph(" "));

            // =========================
            // 🛒 CART
            // =========================
            doc.add(new Paragraph("Cart Items", headerFont));
            doc.add(new Paragraph("-----------------------------------"));

            if (cartItems.isEmpty()) {
                doc.add(new Paragraph("Cart is empty", normalFont));
            } else {
                for (CartItem item : cartItems) {

                    String productName = (item.getProduct() != null)
                            ? item.getProduct().getName()
                            : "Product removed";

                    Double price = (item.getProduct() != null)
                            ? item.getProduct().getBasePrice()
                            : 0.0;

                    doc.add(new Paragraph(productName + " x " + item.getQuantity(), normalFont));
                    doc.add(new Paragraph("Price: $" + price, normalFont));

                    doc.add(new Paragraph(" "));
                }
            }

            doc.add(new Paragraph(" "));

            // =========================
            // 📊 SUMMARY
            // =========================
            doc.add(new Paragraph("Summary", headerFont));
            doc.add(new Paragraph("-----------------------------------"));

            double totalSpent = orders.stream()
                    .mapToDouble(order -> order.getPaidAmount() != null ? order.getPaidAmount() : 0.0)
                    .sum();

            doc.add(new Paragraph("Total Orders: " + orders.size(), normalFont));
            doc.add(new Paragraph("Total Spent: $" + totalSpent, normalFont));

            doc.add(new Paragraph("\n"));

            // =========================
            // 📌 FOOTER
            // =========================
            Paragraph footer = new Paragraph("Generated by Mini Project Backend", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

}