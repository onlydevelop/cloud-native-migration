package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;

// Each method here is its own local transaction. Split into a separate bean
// so OrderSagaOrchestrator invokes them through the Spring proxy - calling
// @Transactional methods via `this.` from within the same class bypasses
// the proxy and silently runs with no transaction at all.
@Service
public class OrderPersistenceSteps {

    private static final Logger log = LoggerFactory.getLogger(OrderPersistenceSteps.class);

    @Autowired private OrderRepository orderRepository;

    @Transactional
    public Order findExisting(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    @Transactional
    public Order createOrder(String idempotencyKey, String customerId, String sku, int quantity, double unitPrice) {
        Order order = new Order();
        order.setIdempotencyKey(idempotencyKey);
        order.setCustomerId(customerId);
        order.setItemSku(sku);
        order.setQuantity(quantity);
        order.setTotalPrice(unitPrice * quantity);
        order.setStatus("CREATED");
        return orderRepository.save(order);
    }

    @Transactional
    public void failOrder(Order order, String reason) {
        order.setStatus("FAILED");
        orderRepository.save(order);
        log.info("Order failed orderId={} reason={}", order.getId(), reason);
    }

    @Transactional
    public void markPaid(Order order) {
        order.setStatus("PAID");
        orderRepository.save(order);
    }

    @Transactional
    public void markShipped(Order order) {
        order.setStatus("SHIPPED");
        orderRepository.save(order);
    }
}
