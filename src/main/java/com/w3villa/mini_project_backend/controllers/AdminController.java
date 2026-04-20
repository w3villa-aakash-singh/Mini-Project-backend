package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.dtos.ProductResponseDTO;
import com.w3villa.mini_project_backend.entites.Category;
import com.w3villa.mini_project_backend.entites.Product;
import com.w3villa.mini_project_backend.repositories.CategoryRepository;
import com.w3villa.mini_project_backend.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;



    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Category> getAllCategories() {
        return categoryRepo.findAll();
    }




    // Product CRUD
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public Product addProduct(@RequestBody Product p) {

        if (p.getCategory() == null || p.getCategory().getId() == null) {
            throw new RuntimeException("Category is required");
        }

        // 🔥 FETCH FROM DB (THIS IS THE FIX)
        Category category = categoryRepo.findById(p.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        p.setCategory(category);

        return productRepo.save(p);
    }

    private ProductResponseDTO mapToDTO(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setBasePrice(product.getBasePrice());
        dto.setImageUrl(product.getImageUrl());

        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        } else {
            dto.setCategoryName("Uncategorized");
        }

        dto.setDiscountPercent(0);
        dto.setDiscountedPrice(product.getBasePrice());

        return dto;
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product p) {
        Product existing = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        // Only update fields if they are provided in the JSON body
        if (p.getName() != null) existing.setName(p.getName());
        if (p.getBasePrice() != null) existing.setBasePrice(p.getBasePrice());
        if (p.getDescription() != null) existing.setDescription(p.getDescription());
        if (p.getImageUrl() != null) existing.setImageUrl(p.getImageUrl());

        // Note: Do not update the ID or Category unless specifically required
        return productRepo.save(existing);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable Long id) { productRepo.deleteById(id); }

    // Category CRUD
    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public Category addCategory(@RequestBody Category c) { return categoryRepo.save(c); }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(@PathVariable Long id) { categoryRepo.deleteById(id); }
}