package com.example.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.order.model.InventoryItem;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
}