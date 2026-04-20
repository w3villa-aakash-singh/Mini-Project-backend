package com.w3villa.mini_project_backend.services;

import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.entites.*;
import com.w3villa.mini_project_backend.dtos.ProductResponseDTO;
import com.w3villa.mini_project_backend.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepo;

    // ✅ 1. Get single product (already exists)
    public ProductResponseDTO getProductWithDiscount(Long productId, User user) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return mapToDTO(product, user);
    }

    // ✅ 2. Get ALL products
    public Page<ProductResponseDTO> getAllProducts(int page, int size, User user) {
        Pageable pageable = PageRequest.of(page, size);

        return productRepo.findAll(pageable)
                .map(product -> mapToDTO(product, user));
    }

    // ✅ 3. Search products
    public List<ProductResponseDTO> searchProducts(String keyword, User user) {
        return productRepo.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(product -> mapToDTO(product, user))
                .toList();
    }

    // ✅ 4. Get products by category (THIS FIXES YOUR ERROR 🔥)
    public List<ProductResponseDTO> getProductsByCategory(Long categoryId, User user) {
        return productRepo.findByCategoryId(categoryId)
                .stream()
                .map(product -> mapToDTO(product, user))
                .toList();
    }

    // ✅ Common mapper method (VERY IMPORTANT)
    private ProductResponseDTO mapToDTO(Product product, User user) {
        ProductResponseDTO dto = new ProductResponseDTO();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBasePrice(product.getBasePrice());
        dto.setCategoryName(product.getCategory().getName());
        dto.setImageUrl(product.getImageUrl());

        int discount = 0;

        if (user != null && user.getPlanType() != null) {


            switch (user.getPlanType()) {
                case SILVER:
                    discount = 10;
                    break;

                case GOLD:
                    discount = 30;
                    break;

                default:
                    discount = 0;
            }
        }

        double discountedPrice = product.getBasePrice() * (1 - discount / 100.0);

        discountedPrice = Math.round(discountedPrice * 100.0) / 100.0;

        dto.setDiscountPercent(discount);
        dto.setDiscountedPrice(discountedPrice);

        return dto;
    }
}