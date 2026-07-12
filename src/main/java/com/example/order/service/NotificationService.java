package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.github.resilience4j.retry.annotation.Retry;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Retry(name = "notification", fallbackMethod = "sendConfirmationFallback")
    @Async
    public void sendConfirmation(String customerId, Long orderId) {
        try {
            Thread.sleep(200); // simulated SMTP round trip
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Notification sent customerId={} orderId={}", customerId, orderId);
    }

    private void sendConfirmationFallback(String customerId, Long orderId, Throwable t) {
        // Log and move on — a failed confirmation email must never fail the order.
        log.error("Notification failed orderId={} error={}", orderId, t.getMessage());
    }
}