package com.example.order.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class NotificationService {

    @Retry(name = "notification", fallbackMethod = "sendConfirmationFallback")
    @Async
    public void sendConfirmation(String customerId, Long orderId) {
        try {
            Thread.sleep(200); // simulated SMTP round trip
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Email sent to " + customerId + " for order " + orderId);
    }

    private void sendConfirmationFallback(String customerId, Long orderId, Throwable t) {
        // Log and move on — a failed confirmation email must never fail the order.
        System.err.println("Notification failed for order " + orderId + ": " + t.getMessage());
    }
}