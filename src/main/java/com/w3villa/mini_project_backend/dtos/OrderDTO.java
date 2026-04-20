package com.w3villa.mini_project_backend.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Double paidAmount;
    private int quantity;
    private String imageUrl; // 🔥 ADD THIS
    private LocalDateTime purchaseDate;
}