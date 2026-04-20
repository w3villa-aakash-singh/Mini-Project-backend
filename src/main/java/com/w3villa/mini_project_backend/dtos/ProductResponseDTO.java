package com.w3villa.mini_project_backend.dtos;

import lombok.Data;

@Data
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Double basePrice;        // Original Price
    private Double discountedPrice;  // Price after 10% or 30% off
    private Integer discountPercent; // To show "30% OFF" label
    private String categoryName;
    private String imageUrl;
}