package com.w3villa.mini_project_backend.controllers;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.w3villa.mini_project_backend.entites.*;
import com.w3villa.mini_project_backend.repositories.*;
import com.w3villa.mini_project_backend.services.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;
    private final UserRepository userRepo;
    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${app.cors.front-end-url}")
    private String frontendUrl;

    @PostConstruct
    public void setupStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    // =========================================
    // ✅ SUCCESS HANDLER (MAIN LOGIC)
    // =========================================
    @GetMapping("/success")
    @Transactional
    public ResponseEntity<String> handleSuccess(
            @RequestParam("session_id") String sessionId) {

        try {
            System.out.println("=== PAYMENT SUCCESS HIT ===");
            System.out.println("SESSION ID: " + sessionId);

            Session session = Session.retrieve(sessionId);
            Map<String, String> metadata = session.getMetadata();

            System.out.println("METADATA: " + metadata);

            if (metadata == null) {
                return ResponseEntity.badRequest().body("No metadata found");
            }

            String type = metadata.get("type");
            String userId = metadata.get("userId");
            String planType = metadata.get("planType");

            System.out.println("TYPE: " + type);
            System.out.println("USER ID: " + userId);
            System.out.println("PLAN TYPE: " + planType);

            if (type == null || userId == null) {
                return ResponseEntity.badRequest().body("Missing metadata");
            }

            // ==========================
            // ✅ PLAN PAYMENT
            // ==========================
            if ("SUBSCRIPTION".equals(type)) {
                userService.upgradeUserPlan(
                        userId,
                        PlanType.valueOf(planType)
                );
            }

            // ==========================
            // ✅ CART PAYMENT
            // ==========================
            if ("CART".equals(type)) {
                User user = userRepo.findById(UUID.fromString(userId))
                        .orElseThrow();

                List<CartItem> cartItems = cartRepo.findByUser(user);

                System.out.println("CART ITEMS: " + cartItems.size());

                for (CartItem item : cartItems) {
                    Order order = new Order();
                    order.setUser(user);
                    Product product = item.getProduct();

                    order.setProductId(product.getId());
                    order.setProductName(product.getName());
                    order.setProductImage(product.getImageUrl());
                    order.setProductPrice(product.getBasePrice());

                    order.setQuantity(item.getQuantity());
                    order.setPaidAmount(product.getBasePrice() * item.getQuantity());
                    order.setStatus("PAID");
                    order.setStripeSessionId(sessionId);
                    order.setPurchaseDate(LocalDateTime.now());
                    order.setPurchaseDate(LocalDateTime.now());

                    orderRepo.save(order);
                }

                cartRepo.deleteByUser(user);
            }

            // ==========================
            // ✅ PRODUCT PAYMENT
            // ==========================
            if ("PRODUCT".equals(type)) {

                String productId = metadata.get("productId");

                User user = userRepo.findById(UUID.fromString(userId))
                        .orElseThrow();

                Product product = productRepo.findById(Long.valueOf(productId))
                        .orElseThrow();

                Order order = Order.builder()
                        .user(user)

                        // ✅ SNAPSHOT DATA
                        .productId(product.getId())
                        .productName(product.getName())
                        .productImage(product.getImageUrl())
                        .productPrice(product.getBasePrice())

                        .quantity(1)
                        .paidAmount(product.getBasePrice())
                        .status("PAID")
                        .stripeSessionId(sessionId)
                        .purchaseDate(LocalDateTime.now())
                        .build();

                orderRepo.save(order);
            }

            return ResponseEntity.ok("Payment processed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Payment failed");
        }
    }

    // =========================================
    // ✅ CREATE PLAN SESSION
    // =========================================
    @PostMapping("/create-plan-session")
    public ResponseEntity<Map<String, String>> createPlanSession(
            @RequestBody Map<String, String> request) throws Exception {

        long amount = request.get("planName").equalsIgnoreCase("GOLD") ? 1000L : 500L;

        Session session = Session.create(
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(frontendUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(frontendUrl + "/plans")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount(amount)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(request.get("planName"))
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata("type", "SUBSCRIPTION")
                        .putMetadata("userId", request.get("userId"))
                        .putMetadata("planType", request.get("planName").toUpperCase())
                        .build()
        );

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    // =========================================
    // ✅ CREATE CART SESSION
    // =========================================
    @PostMapping("/create-cart-session")
    public ResponseEntity<Map<String, String>> createCartSession(
            @RequestBody Map<String, String> request) throws Exception {

        Session session = Session.create(
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(frontendUrl + "/cart-success?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(frontendUrl + "/cart")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount(Long.parseLong(request.get("totalAmount")))
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Cart Payment")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .putMetadata("type", "CART")
                        .putMetadata("userId", request.get("userId"))
                        .build()
        );

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @PostMapping("/create-product-session")
    public ResponseEntity<Map<String, String>> createProductSession(
            @RequestBody Map<String, String> request
    ) throws Exception {

        Long productId = Long.parseLong(request.get("productId"));
        String userId = request.get("userId");

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Session session = Session.create(
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(frontendUrl + "/product-success?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(frontendUrl + "/products")

                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount((long) (product.getBasePrice() * 100))
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName(product.getName())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )

                        // 🔥 VERY IMPORTANT METADATA
                        .putMetadata("type", "PRODUCT")
                        .putMetadata("userId", userId)
                        .putMetadata("productId", productId.toString())
                        .putMetadata("price", product.getBasePrice().toString())

                        .build()
        );

        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }
}