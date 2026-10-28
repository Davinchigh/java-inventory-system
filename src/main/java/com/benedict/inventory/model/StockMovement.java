package com.benedict.inventory.model;

import java.time.LocalDateTime;

/**
 * One change to a product's stock, kept so there's a history of what came in and went out.
 *
 * @param change positive for stock received, negative for stock issued
 */
public record StockMovement(LocalDateTime time, String sku, Type type, int change, int balanceAfter, String note) {

    public enum Type {
        ADDED("New product"),
        STOCK_IN("Stock in"),
        STOCK_OUT("Stock out"),
        REMOVED("Product removed");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
