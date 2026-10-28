package com.benedict.inventory.service;

import com.benedict.inventory.model.Product;
import com.benedict.inventory.model.StockMovement;
import com.benedict.inventory.model.StockMovement.Type;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * The stock list and every rule about changing it.
 *
 * The UI and the storage layer only talk to this class, so all validation lives in one place
 * and every change is recorded as a {@link StockMovement}.
 */
public class Inventory {

    private final Map<String, Product> products = new LinkedHashMap<>();
    private final List<StockMovement> history = new ArrayList<>();
    private final Clock clock;

    public Inventory() {
        this(Clock.systemDefaultZone());
    }

    /** The clock is injectable so tests can control timestamps. */
    public Inventory(Clock clock) {
        this.clock = clock;
    }

    // ---- Changes ----

    public Product addProduct(Product product) {
        if (products.containsKey(product.getSku())) {
            throw new IllegalArgumentException("A product with SKU " + product.getSku() + " already exists.");
        }
        products.put(product.getSku(), product);
        record(product, Type.ADDED, product.getQuantity(), "Opening stock");
        return product;
    }

    public void updateDetails(String sku, String name, String category, int reorderLevel, BigDecimal unitPrice) {
        Product product = require(sku);
        product.setName(name);
        product.setCategory(category);
        product.setReorderLevel(reorderLevel);
        product.setUnitPrice(unitPrice);
    }

    public void stockIn(String sku, int amount, String note) {
        Product product = require(sku);
        product.addStock(amount);
        record(product, Type.STOCK_IN, amount, note);
    }

    public void stockOut(String sku, int amount, String note) {
        Product product = require(sku);
        product.removeStock(amount);
        record(product, Type.STOCK_OUT, -amount, note);
    }

    public void removeProduct(String sku) {
        Product product = require(sku);
        products.remove(product.getSku());
        record(product, Type.REMOVED, -product.getQuantity(), "Removed from inventory");
    }

    // ---- Queries ----

    public Optional<Product> find(String sku) {
        return sku == null ? Optional.empty() : Optional.ofNullable(products.get(sku.trim().toUpperCase()));
    }

    public List<Product> all() {
        return List.copyOf(products.values());
    }

    /** Matches the query against SKU, name and category, ignoring case. A blank query returns everything. */
    public List<Product> search(String query) {
        if (query == null || query.isBlank()) {
            return all();
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        return products.values().stream()
                .filter(p -> p.getSku().toLowerCase(Locale.ROOT).contains(q)
                        || p.getName().toLowerCase(Locale.ROOT).contains(q)
                        || p.getCategory().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    /** Products at or below their reorder level, the emptiest first. */
    public List<Product> lowStock() {
        return products.values().stream()
                .filter(Product::isLowStock)
                .sorted(Comparator.comparingInt(Product::getQuantity))
                .toList();
    }

    public List<String> categories() {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        products.values().forEach(p -> names.add(p.getCategory()));
        return List.copyOf(names);
    }

    public BigDecimal totalValue() {
        return products.values().stream().map(Product::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int totalUnits() {
        return products.values().stream().mapToInt(Product::getQuantity).sum();
    }

    public List<StockMovement> history() {
        return Collections.unmodifiableList(history);
    }

    // ---- Loading saved data (no new history entries) ----

    public void restore(List<Product> savedProducts, List<StockMovement> savedHistory) {
        products.clear();
        history.clear();
        savedProducts.forEach(p -> products.put(p.getSku(), p));
        history.addAll(savedHistory);
    }

    // ---- Helpers ----

    private Product require(String sku) {
        return find(sku).orElseThrow(() -> new IllegalArgumentException("No product with SKU " + sku + "."));
    }

    private void record(Product product, Type type, int change, String note) {
        int balance = type == Type.REMOVED ? 0 : product.getQuantity();
        String cleanNote = note == null ? "" : note.trim();
        history.add(new StockMovement(LocalDateTime.now(clock), product.getSku(), type, change, balance, cleanNote));
    }
}
