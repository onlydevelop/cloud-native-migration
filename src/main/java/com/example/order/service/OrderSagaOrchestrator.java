package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.order.model.Order;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    @Autowired private OrderPersistenceSteps persistenceSteps;
    @Autowired private InventoryService inventoryService;
    @Autowired private PaymentService paymentService;
    @Autowired private NotificationService notificationService;

    // NOTE: no @Transactional at this level. This method orchestrates several
    // independently-committed steps (see OrderPersistenceSteps). Each step
    // below manages its own transaction.
    public Order placeOrder(String idempotencyKey, String customerId, String sku, int quantity, double unitPrice) {

        Order existing = persistenceSteps.findExisting(idempotencyKey);
        if (existing != null) return existing;

        // --- Step 1: create order (own local transaction) ---
        Order order = persistenceSteps.createOrder(idempotencyKey, customerId, sku, quantity, unitPrice);

        // --- Step 2: reserve inventory (own local transaction) ---
        boolean reserved = inventoryService.reserve(sku, quantity);
        if (!reserved) {
            persistenceSteps.failOrder(order, "INVENTORY_UNAVAILABLE");
            return order;
        }

        // --- Step 3: charge payment (external call, NOT wrapped in a DB transaction) ---
        boolean charged;
        try {
            charged = paymentService.charge(idempotencyKey, customerId, order.getTotalPrice()).get();
        } catch (Exception e) {
            log.error("Payment call threw for orderId={}: {}", order.getId(), e.getMessage());
            charged = false;
        }

        if (!charged) {
            // COMPENSATION: undo step 2, since step 3 failed
            log.warn("Compensating: releasing inventory for orderId={} after payment failure", order.getId());
            inventoryService.release(sku, quantity);
            persistenceSteps.failOrder(order, "PAYMENT_DECLINED");
            return order;
        }

        // --- Step 4: mark paid (own local transaction) ---
        persistenceSteps.markPaid(order);

        // --- Step 5: notify (fire-and-forget, no compensation needed) ---
        notificationService.sendConfirmation(customerId, order.getId());

        // --- Step 6: mark shipped (own local transaction) ---
        persistenceSteps.markShipped(order);

        return order;
    }
}
