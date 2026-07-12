package com.example.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.order.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}