package com.w3villa.mini_project_backend.controllers;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.w3villa.mini_project_backend.entites.PlanType;
import com.w3villa.mini_project_backend.services.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;

    @Value("${stripe.webhook-secret}")
    private String endpointSecret;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${app.cors.front-end-url}")
    private String frontendUrl;

    /**
     * Initializes the Stripe SDK globally on application startup.
     */
    @PostConstruct
    public void setupStripe() {
        Stripe.apiKey = stripeSecretKey;
        System.out.println("✅ STRIPE SYSTEM ONLINE: SDK Initialized.");
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, String> request) throws StripeException {
        String planName = request.get("planName");
        String userId = request.get("userId");

        // Basic validation to prevent null metadata
        if (userId == null || planName == null) {
            System.err.println("❌ REJECTED: userId or planName is null in the request");
            return ResponseEntity.badRequest().build();
        }

        long priceInCents = planName.equalsIgnoreCase("GOLD") ? 1000L : 500L;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/profile?payment_success=true")
                .setCancelUrl(frontendUrl + "/plans?payment_failed=true")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(priceInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(planName.toUpperCase() + " PROTOCOL ACCESS")
                                        .setDescription("Identity Verification Upgrade")
                                        .build())
                                .build())
                        .build())
                // METADATA: This is crucial for the webhook to identify the user
                .putMetadata("userId", userId)
                .putMetadata("planType", planName.toUpperCase())
                .build();

        Session session = Session.create(params);

        Map<String, String> response = new HashMap<>();
        response.put("url", session.getUrl());

        System.out.println("DEBUG: Created Stripe Session for User: " + userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        // If you don't see THIS, your Networking/Security is the problem
        System.out.println("🚨 SIGNAL REACHED CONTROLLER");

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

            // Use the RAW Map instead of the Deserializer
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getData().getObject();
                Map<String, String> metadata = session.getMetadata();

                String userId = metadata.get("userId");
                String plan = metadata.get("planType");

                System.out.println("🎯 TARGET ACQUIRED: " + userId + " | PLAN: " + plan);

                if (userId != null) {
                    userService.upgradeUserPlan(userId, PlanType.valueOf(plan));
                }
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            System.out.println("❌ WEBHOOK CRASHED: " + e.getMessage());
            return ResponseEntity.status(400).body("Error");
        }
    }
}