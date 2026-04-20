package com.w3villa.mini_project_backend.entites;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // USER
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    // ❌ REMOVE THIS
    // private Product product;

    // ✅ ADD THIS (VERY IMPORTANT)
    private Long productId;
    private String productName;
    private String productImage;
    private Double productPrice;

    private int quantity;
    private Double paidAmount;
    private String status;
    private String stripeSessionId;

    private LocalDateTime purchaseDate = LocalDateTime.now();
}