package com.example.order.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // TimeLimiter requires the method to return a CompletableFuture/async type.
    // Order of annotations matters: TimeLimiter wraps innermost (per-call),
    // Retry wraps that (re-attempts on failure/timeout),
    // CircuitBreaker wraps outermost (tracks aggregate failure rate across retries).
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "chargeFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<Boolean> charge(String customerId, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300); // simulated network latency
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return amount < 10000;
        });
    }

    // Fallback signature must mirror the original method + a Throwable param.
    // Called when retries are exhausted OR the circuit is open.
    private CompletableFuture<Boolean> chargeFallback(String customerId, double amount, Throwable t) {
        // Fail closed: treat as declined, never silently treat a failed/unknown
        // payment call as success. This is a business-correctness decision, not
        // just a technical one.
        log.warn("Payment fallback triggered for customer={} amount={} reason={}",
                customerId, amount, t.getMessage());
        return CompletableFuture.completedFuture(false);
    }
}