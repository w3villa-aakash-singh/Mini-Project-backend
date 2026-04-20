package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.entites.CartItem;
import com.w3villa.mini_project_backend.entites.Product;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.CartRepository;
import com.w3villa.mini_project_backend.repositories.ProductRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    private User getUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }

    // ✅ GET CART
    @GetMapping
    public List<CartItem> getCart(Authentication auth) {
        User user = getUser(auth);
        return cartRepo.findByUser(user);
    }

    // ✅ ADD TO CART
    @PostMapping
    public CartItem addToCart(
            @RequestParam Long productId,
            Authentication auth
    ) {
        User user = getUser(auth);
        Product product = productRepo.findById(productId).orElseThrow();

        Optional<CartItem> existing = cartRepo.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + 1);
            return cartRepo.save(item);
        }

        CartItem item = CartItem.builder()
                .user(user)
                .product(product)
                .quantity(1)
                .build();

        return cartRepo.save(item);
    }

    // ✅ UPDATE QTY
    @PutMapping("/{id}")
    public CartItem updateQty(
            @PathVariable Long id,
            @RequestParam int qty
    ) {
        CartItem item = cartRepo.findById(id).orElseThrow();
        item.setQuantity(qty);
        return cartRepo.save(item);
    }

    // ✅ DELETE ITEM
    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable Long id) {
        cartRepo.deleteById(id);
    }

    // ✅ CLEAR CART
    @DeleteMapping("/clear")
    public void clearCart(Authentication auth) {
        User user = getUser(auth);
        cartRepo.deleteByUser(user);
    }
}