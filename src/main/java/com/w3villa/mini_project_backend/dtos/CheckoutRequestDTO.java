package com.w3villa.mini_project_backend.dtos;

import lombok.*;
import java.util.List;

@Getter @Setter
public class CheckoutRequestDTO {
    private List<CartItem> items;

    @Getter @Setter
    public static class CartItem {
        private Long productId;
        private int quantity;
    }
}