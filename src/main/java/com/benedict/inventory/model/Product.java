package com.benedict.inventory.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A product held in stock.
 *
 * The SKU is the product's identity: two products with the same SKU are the same product.
 * Quantity can only change through {@link #addStock(int)} and {@link #removeStock(int)},
 * so stock can never go negative.
 */
public class Product {

    private final String sku;
    private String name;
    private String category;
    private int quantity;
    private int reorderLevel;
    private BigDecimal unitPrice;

    public Product(String sku, String name, String category, int quantity, int reorderLevel, BigDecimal unitPrice) {
        this.sku = requireText(sku, "SKU").toUpperCase();
        setName(name);
        setCategory(category);
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity can't be negative.");
        }
        this.quantity = quantity;
        setReorderLevel(reorderLevel);
        setUnitPrice(unitPrice);
    }

    // ---- Stock changes ----

    public void addStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to add must be more than zero.");
        }
        quantity += amount;
    }

    public void removeStock(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to remove must be more than zero.");
        }
        if (amount > quantity) {
            throw new IllegalStateException(
                    "Only " + quantity + " of " + name + " in stock, can't remove " + amount + ".");
        }
        quantity -= amount;
    }

    /** True when stock has fallen to or below the reorder level. */
    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }

    /** Quantity multiplied by unit price. */
    public BigDecimal stockValue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ---- Getters and validated setters ----

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "Name");
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = (category == null || category.isBlank()) ? "General" : category.trim();
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level can't be negative.");
        }
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        Objects.requireNonNull(unitPrice, "Unit price is required.");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price can't be negative.");
        }
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Product other && sku.equals(other.sku));
    }

    @Override
    public int hashCode() {
        return sku.hashCode();
    }

    @Override
    public String toString() {
        return sku + " " + name + " (" + quantity + ")";
    }
}
