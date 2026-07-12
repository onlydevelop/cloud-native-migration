package com.example.order.service;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private PaymentService paymentService;
    @Autowired private NotificationService notificationService;

    // One giant transactional method doing inventory + payment + notification + persistence.
    // Any downstream failure (email, payment) risks rolling back or half-completing the order.
    @Transactional
    public Order placeOrder(String customerId, String sku, int quantity, double unitPrice) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setItemSku(sku);
        order.setQuantity(quantity);
        order.setTotalPrice(unitPrice * quantity);
        order.setStatus("CREATED");
        orderRepository.save(order);

        boolean reserved = inventoryService.reserve(sku, quantity);
        if (!reserved) {
            order.setStatus("FAILED");
            orderRepository.save(order);
            return order;
        }

        boolean charged;
        try {
            // The circuit breaker/retry/fallback chain on charge() means this
            // future always resolves rather than throwing for gateway failures,
            // but propagate anything unexpected instead of masking it.
            charged = paymentService.charge(customerId, order.getTotalPrice()).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for payment gateway", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Payment gateway call failed unexpectedly", e);
        }
        if (!charged) {
            inventoryService.release(sku, quantity);
            order.setStatus("FAILED");
            orderRepository.save(order);
            return order;
        }

        order.setStatus("PAID");
        orderRepository.save(order);

        // Synchronous, in-request notification — blocks the caller.
        notificationService.sendConfirmation(customerId, order.getId());

        order.setStatus("SHIPPED");
        orderRepository.save(order);
        return order;
    }
}