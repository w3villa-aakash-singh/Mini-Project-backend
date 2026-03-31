package com.w3villa.mini_project_backend.controllers;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.w3villa.mini_project_backend.entites.PlanType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.w3villa.mini_project_backend.services.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;

    @Value("${stripe.webhook-secret}") // Matches taskyour YML key
    private String endpointSecret;

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${app.cors.front-end-url}")
    private String frontendUrl;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            System.out.println("DEBUG: Event Received: " + event.getType());

            if ("checkout.session.completed".equals(event.getType())) {
                // 1. GET THE RAW OBJECT DATA
                // Instead of using the Deserializer, we cast the data object directly
                // This bypasses the deserialization error you are seeing
                Session session = (Session) event.getData().getObject();

                if (session != null) {
                    String userId = session.getMetadata().get("userId");
                    String planName = session.getMetadata().get("planType");

                    System.out.println("DEBUG: Metadata Found - User: " + userId + " Plan: " + planName);

                    if (userId != null && planName != null) {
                        userService.upgradeUserPlan(userId, PlanType.valueOf(planName.toUpperCase()));
                        return ResponseEntity.ok("Database Updated");
                    } else {
                        System.err.println("❌ ERROR: Metadata missing in Session!");
                    }
                }
            }
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            System.err.println("❌ WEBHOOK ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, String> request) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String planName = request.get("planName"); // "SILVER" or "GOLD"
        String userId = request.get("userId");

        // Set price based on PlanType (Price in Cents: 500 = $5.00)
        long priceInCents = planName.equalsIgnoreCase("GOLD") ? 1000L : 500L;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                // Redirect back to profile with a success flag
                .setSuccessUrl(frontendUrl + "/dashboard?payment_success=true")
                .setCancelUrl(frontendUrl + "/plans?payment_failed=true")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(priceInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(planName + " PROTOCOL ACCESS")
                                        .setDescription("Limited time access for your profile dossier.")
                                        .build())
                                .build())
                        .build())
                // METADATA: Crucial for identifying the user when the payment finishes
                .putMetadata("userId", userId)
                .putMetadata("planType", planName.toUpperCase())
                .build();

        Session session = Session.create(params);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("url", session.getUrl()); // This is the URL React will redirect to
        return ResponseEntity.ok(responseData);
    }
}