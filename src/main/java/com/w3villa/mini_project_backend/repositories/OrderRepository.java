package com.w3villa.mini_project_backend.repositories;

import com.w3villa.mini_project_backend.entites.Order;
import com.w3villa.mini_project_backend.entites.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
}