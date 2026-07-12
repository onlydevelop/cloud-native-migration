package com.example.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.order.model.Order;
import com.example.order.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @RequestParam String customerId,
                                     @RequestParam String sku,
                                     @RequestParam int quantity,
                                     @RequestParam double unitPrice) {
    Order order = orderService.placeOrder(idempotencyKey, customerId, sku, quantity, unitPrice);
    return ResponseEntity.status(order.getStatus().equals("FAILED") ? 422 : 200).body(order);
}
}