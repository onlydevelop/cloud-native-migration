package com.example.order.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private PaymentService paymentService;
    @Autowired private NotificationService notificationService;
    @Autowired private MeterRegistry meterRegistry;

    // One giant transactional method doing inventory + payment + notification + persistence.
    // Any downstream failure (email, payment) risks rolling back or half-completing the order.
    @Transactional
    public Order placeOrder(String idempotencyKey, String customerId, String sku, int quantity, double unitPrice) {
        Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate request detected idempotencyKey={} orderId={}", idempotencyKey, existing.get().getId());
            return existing.get(); // return the original result, do NOT re-process
        }
        Order order = new Order();
        order.setIdempotencyKey(idempotencyKey);
        order.setCustomerId(customerId);
        order.setItemSku(sku);
        order.setQuantity(quantity);
        order.setTotalPrice(unitPrice * quantity);
        order.setStatus("CREATED");
        try {
            orderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests with the same key both passed the findBy check above —
            // the unique constraint is what actually prevents the double-create, not this Java code.
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
        }

        boolean reserved = inventoryService.reserve(sku, quantity);
        if (!reserved) {
            order.setStatus("FAILED");
            recordOrderOutcome("FAILED");
            log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
            orderRepository.save(order);
            return order;
        }

        boolean charged;
        try {
            charged = paymentService.charge(customerId, order.getTotalPrice()).get(); // blocks up to timeout+retries
        } catch (Exception e) {
            charged = false;
        }

        if (!charged) {
            inventoryService.release(sku, quantity);
            order.setStatus("FAILED");
            recordOrderOutcome("FAILED");
            log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
            orderRepository.save(order);
            return order;
        }

        order.setStatus("PAID");
        recordOrderOutcome("PAID");
        log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
        orderRepository.save(order);

        notificationService.sendConfirmation(customerId, order.getId()); // fire-and-forget now

        order.setStatus("SHIPPED");
        recordOrderOutcome("SHIPPED");
        log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
        orderRepository.save(order);
        return order;
    }

    private void recordOrderOutcome(String status) {
        meterRegistry.counter("orders.processed", "status", status).increment();
    }
}