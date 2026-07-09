package com.example.order.controller;

import com.example.order.model.Order;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderService orderService;

    @PostMapping
    public Order create(@RequestParam String customerId,
                         @RequestParam String sku,
                         @RequestParam int quantity,
                         @RequestParam double unitPrice) {
        return orderService.placeOrder(customerId, sku, quantity, unitPrice);
    }
}