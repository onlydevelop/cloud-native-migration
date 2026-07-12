package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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
            log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
            orderRepository.save(order);
            return order;
        }

        order.setStatus("PAID");
        log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
        orderRepository.save(order);

        notificationService.sendConfirmation(customerId, order.getId()); // fire-and-forget now

        order.setStatus("SHIPPED");
        log.info("Order status transition orderId={} status={}", order.getId(), order.getStatus());
        orderRepository.save(order);
        return order;
    }
}