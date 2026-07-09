package com.example.order.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// In-memory "inventory" — lives and dies with this JVM. No persistence, no locking across instances.
@Service
public class InventoryService {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    public InventoryService() {
        stock.put("SKU-100", 50);
        stock.put("SKU-200", 10);
    }

    public boolean reserve(String sku, int qty) {
        synchronized (this) {
            Integer available = stock.get(sku);
            if (available == null || available < qty) {
                return false;
            }
            stock.put(sku, available - qty);
            return true;
        }
    }

    public void release(String sku, int qty) {
        stock.merge(sku, qty, Integer::sum);
    }
}