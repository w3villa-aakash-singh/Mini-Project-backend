package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom query to find all products belonging to a specific category
    List<Product> findByCategoryId(Long categoryId);

    // Search products by name (useful for the frontend search bar)
    List<Product> findByNameContainingIgnoreCase(String name);
}