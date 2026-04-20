package com.w3villa.mini_project_backend.controllers;

import com.w3villa.mini_project_backend.dtos.OrderDTO;
import com.w3villa.mini_project_backend.entites.User;
import com.w3villa.mini_project_backend.repositories.OrderRepository;
import com.w3villa.mini_project_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepo;
    private final UserRepository userRepository;

    @GetMapping
    public List<OrderDTO> getMyOrders(Authentication auth) {

        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return orderRepo.findByUser(user)
                .stream()
                .map(order -> {
                    OrderDTO dto = new OrderDTO();

                    dto.setId(order.getId());
                    dto.setProductId(order.getProductId());
                    dto.setProductName(order.getProductName());
                    dto.setImageUrl(order.getProductImage());
                    dto.setPaidAmount(order.getPaidAmount());
                    dto.setQuantity(order.getQuantity());
                    dto.setPurchaseDate(order.getPurchaseDate());

                    return dto;
                })
                .toList();
    }
}