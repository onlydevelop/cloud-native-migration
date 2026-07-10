package com.example.order.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class InventoryItem {
    @Id
    private String sku;

    private int availableQty;

    @Version // optimistic locking — prevents lost updates under concurrent writers
    private Long version;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}