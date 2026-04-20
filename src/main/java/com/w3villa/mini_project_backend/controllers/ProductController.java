package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.dtos.ProductResponseDTO;
import com.w3villa.mini_project_backend.entites.Category;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.CategoryRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import com.w3villa.mini_project_backend.services.ProductService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // =========================================
    // ✅ CONFIRM CONTROLLER IS LOADED
    // =========================================
    @PostConstruct
    public void init() {
        System.out.println("🚀 ProductController LOADED");
    }

    // =========================================
    // ✅ GET LOGGED-IN USER (FINAL FIX)
    // =========================================
    private User getLoggedInUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                System.out.println("❌ No Authentication found");
                return null;
            }

            String identifier = auth.getName(); // 🔥 could be UUID OR email


            if (identifier == null) return null;

            // 🔥 Try UUID first
            try {
                UUID uuid = UUID.fromString(identifier);
                return userRepository.findById(uuid).orElse(null);
            } catch (Exception e) {
                // 🔥 fallback → email
                return userRepository.findByEmail(identifier).orElse(null);
            }

        } catch (Exception e) {
            return null;
        }
    }

    // =========================================
    // ✅ GET ALL CATEGORIES
    // =========================================
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    // =========================================
    // ✅ GET PRODUCTS BY CATEGORY
    // =========================================
    @GetMapping("/category/{id}")
    public ResponseEntity<List<ProductResponseDTO>> getByCategory(
            @PathVariable Long id
    ) {


        User user = getLoggedInUser();



        return ResponseEntity.ok(productService.getProductsByCategory(id, user));
    }

    // =========================================
    // ✅ SEARCH PRODUCTS
    // =========================================
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(
            @RequestParam String keyword
    ) {


        User user = getLoggedInUser();

        return ResponseEntity.ok(productService.searchProducts(keyword, user));
    }

    // =========================================
    // ✅ GET ALL PRODUCTS (MAIN API)
    // =========================================
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        User user = getLoggedInUser();



        return ResponseEntity.ok(productService.getAllProducts(page, size, user));
    }

    // =========================================
    // ✅ GET SINGLE PRODUCT
    // =========================================
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProduct(
            @PathVariable Long id
    ) {

        User user = getLoggedInUser();



        return ResponseEntity.ok(productService.getProductWithDiscount(id, user));
    }
}