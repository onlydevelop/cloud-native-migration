package com.example.order.service;

import org.springframework.stereotype.Service;

// Simulates a blocking, synchronous call to an external payment gateway.
// No timeout, no retry, no circuit breaker — a single slow dependency stalls every order.
@Service
public class PaymentService {

    public boolean charge(String customerId, double amount) {
        try {
            Thread.sleep(300); // simulated network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // fake success rule
        return amount < 10000;
    }
}