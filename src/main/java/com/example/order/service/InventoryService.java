package com.example.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.order.model.InventoryItem;
import com.example.order.repository.InventoryRepository;

@Service
public class InventoryService {

    @Autowired private InventoryRepository inventoryRepository;

    // Now backed by the DB row + @Version, so concurrent instances
    // can't both "succeed" reserving the same last unit.
    @Transactional
    public boolean reserve(String sku, int qty) {
        InventoryItem item = inventoryRepository.findById(sku)
                .orElseThrow(() -> new IllegalArgumentException("Unknown SKU: " + sku));

        if (item.getAvailableQty() < qty) {
            return false;
        }

        item.setAvailableQty(item.getAvailableQty() - qty);
        try {
            inventoryRepository.save(item);
        } catch (OptimisticLockingFailureException e) {
            // another instance/thread updated this row first — caller should retry
            return false;
        }
        return true;
    }

    @Transactional
    public void release(String sku, int qty) {
        InventoryItem item = inventoryRepository.findById(sku)
                .orElseThrow(() -> new IllegalArgumentException("Unknown SKU: " + sku));
        item.setAvailableQty(item.getAvailableQty() + qty);
        inventoryRepository.save(item);
    }
}