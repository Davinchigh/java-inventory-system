package com.benedict.inventory;

import com.benedict.inventory.model.Product;
import com.benedict.inventory.model.StockMovement;
import com.benedict.inventory.service.Inventory;
import com.benedict.inventory.storage.CsvInventoryStore;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain Java tests, no libraries needed. Run with ./test.sh (or test.bat on Windows).
 * Each test is a method; a failed check throws and is reported at the end.
 */
public final class InventoryTests {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-01-15T09:00:00Z"), ZoneOffset.UTC);
    private static final List<String> failures = new ArrayList<>();
    private static int passed;

    public static void main(String[] args) throws Exception {
        run("SKU is stored in upper case and identifies the product", InventoryTests::skuIdentity);
        run("Stock in and out change quantity and history", InventoryTests::stockInAndOut);
        run("Stock can't go below zero", InventoryTests::noNegativeStock);
        run("Duplicate SKUs are rejected", InventoryTests::duplicateSku);
        run("Low stock list is sorted emptiest first", InventoryTests::lowStock);
        run("Search matches SKU, name and category", InventoryTests::search);
        run("Total value adds up every product", InventoryTests::totalValue);
        run("Invalid product details are rejected", InventoryTests::validation);
        run("Saving and loading keeps products, commas and history", InventoryTests::roundTrip);

        System.out.println();
        System.out.println(passed + " passed, " + failures.size() + " failed");
        failures.forEach(f -> System.out.println("  FAILED: " + f));
        if (!failures.isEmpty()) System.exit(1);
    }

    // ---- Tests ----

    static void skuIdentity() {
        Product p = product("abc-1", 5, 2, "10");
        check(p.getSku().equals("ABC-1"), "SKU should be upper case");
        check(p.equals(product("ABC-1", 99, 0, "1")), "same SKU should be equal");
    }

    static void stockInAndOut() {
        Inventory inv = new Inventory(FIXED);
        inv.addProduct(product("P1", 10, 2, "5"));
        inv.stockIn("p1", 5, "Delivery");
        inv.stockOut("P1", 12, "Sale");
        check(inv.find("P1").orElseThrow().getQuantity() == 3, "10 + 5 - 12 should leave 3");

        List<StockMovement> h = inv.history();
        check(h.size() == 3, "expected 3 history entries, got " + h.size());
        check(h.get(2).change() == -12 && h.get(2).balanceAfter() == 3, "last entry should be -12 leaving 3");
        check(h.get(1).note().equals("Delivery"), "note should be kept");
    }

    static void noNegativeStock() {
        Inventory inv = new Inventory(FIXED);
        inv.addProduct(product("P1", 2, 0, "5"));
        expect(IllegalStateException.class, () -> inv.stockOut("P1", 3, ""));
        check(inv.find("P1").orElseThrow().getQuantity() == 2, "quantity should be unchanged after a refused stock out");
        check(inv.history().size() == 1, "a refused stock out shouldn't be recorded");
        expect(IllegalArgumentException.class, () -> inv.stockIn("P1", 0, ""));
    }

    static void duplicateSku() {
        Inventory inv = new Inventory(FIXED);
        inv.addProduct(product("P1", 1, 0, "1"));
        expect(IllegalArgumentException.class, () -> inv.addProduct(product("p1", 1, 0, "1")));
    }

    static void lowStock() {
        Inventory inv = new Inventory(FIXED);
        inv.addProduct(product("A", 10, 5, "1"));
        inv.addProduct(product("B", 4, 5, "1"));
        inv.addProduct(product("C", 0, 5, "1"));
        inv.addProduct(product("D", 5, 5, "1"));
        List<String> low = inv.lowStock().stream().map(Product::getSku).toList();
        check(low.equals(List.of("C", "B", "D")), "expected [C, B, D], got " + low);
    }

    static void search() {
        Inventory inv = new Inventory(FIXED);
        inv.addProduct(new Product("STA-1", "Exercise book", "Stationery", 1, 0, BigDecimal.ONE));
        inv.addProduct(new Product("ELE-1", "Calculator", "Electronics", 1, 0, BigDecimal.ONE));
        check(inv.search("sta").size() == 1, "SKU search");
        check(inv.search("CALC").size() == 1, "name search ignores case");
        check(inv.search("electronics").size() == 1, "category search");
        check(inv.search("  ").size() == 2, "blank search returns everything");
    }

    static void totalValue() {
        Inventory inv = new Inventory(FIXED);
        inv.addProduct(product("A", 3, 0, "2.50"));
        inv.addProduct(product("B", 2, 0, "10"));
        check(inv.totalValue().compareTo(new BigDecimal("27.50")) == 0, "3 × 2.50 + 2 × 10 = 27.50, got " + inv.totalValue());
        check(inv.totalUnits() == 5, "5 units in total");
    }

    static void validation() {
        expect(IllegalArgumentException.class, () -> product(" ", 1, 0, "1"));
        expect(IllegalArgumentException.class, () -> product("X", -1, 0, "1"));
        expect(IllegalArgumentException.class, () -> product("X", 1, -1, "1"));
        expect(IllegalArgumentException.class, () -> product("X", 1, 0, "-1"));
        check(new Product("X", "Thing", "  ", 1, 0, BigDecimal.ONE).getCategory().equals("General"),
                "blank category should become General");
    }

    static void roundTrip() throws Exception {
        Path folder = Files.createTempDirectory("inventory-test");
        Inventory original = new Inventory(FIXED);
        original.addProduct(new Product("Q1", "Pens, blue \"fine\"", "Stationery", 7, 2, new BigDecimal("3.5")));
        original.stockOut("Q1", 2, "Sold, cash");
        new CsvInventoryStore(folder).save(original);

        Inventory loaded = new Inventory(FIXED);
        new CsvInventoryStore(folder).load(loaded);
        Product p = loaded.find("Q1").orElseThrow();
        check(p.getName().equals("Pens, blue \"fine\""), "name with comma and quotes should survive, got " + p.getName());
        check(p.getQuantity() == 5, "quantity should be 5");
        check(p.getUnitPrice().equals(new BigDecimal("3.50")), "price should be 3.50");
        check(loaded.history().size() == 2, "history should have 2 entries");
        check(loaded.history().get(1).note().equals("Sold, cash"), "note with comma should survive");
    }

    // ---- Tiny test harness ----

    private interface Test {
        void run() throws Exception;
    }

    private static void run(String name, Test test) {
        try {
            test.run();
            passed++;
            System.out.println("  ok  " + name);
        } catch (Throwable t) {
            failures.add(name + ": " + t.getMessage());
            System.out.println("  XX  " + name);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Test code) {
        try {
            code.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    private static Product product(String sku, int qty, int reorder, String price) {
        return new Product(sku, "Item " + sku, "Test", qty, reorder, new BigDecimal(price));
    }
}
