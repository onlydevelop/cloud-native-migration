package com.example.order.service;

import org.springframework.stereotype.Service;

// Fire-and-forget email, but called synchronously inline with the order flow.
// If this hangs, the customer's HTTP request hangs with it.
@Service
public class NotificationService {

    public void sendConfirmation(String customerId, Long orderId) {
        try {
            Thread.sleep(200); // simulated SMTP round trip
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Email sent to " + customerId + " for order " + orderId);
    }
}