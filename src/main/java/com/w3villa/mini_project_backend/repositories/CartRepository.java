package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.CartItem;
import com.w3villa.mini_project_backend.entites.Product;
import com.w3villa.mini_project_backend.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);
}