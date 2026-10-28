package com.benedict.inventory.storage;

import com.benedict.inventory.model.Product;
import com.benedict.inventory.model.StockMovement;
import com.benedict.inventory.service.Inventory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves the inventory as two CSV files so the data opens in Excel too:
 * products.csv and history.csv in the given folder.
 *
 * Writes go to a temporary file first and are then moved into place,
 * so a crash halfway through a save can't leave a half-written file.
 */
public class CsvInventoryStore {

    private static final String PRODUCTS_HEADER = "sku,name,category,quantity,reorder_level,unit_price";
    private static final String HISTORY_HEADER = "time,sku,type,change,balance_after,note";

    private final Path productsFile;
    private final Path historyFile;

    public CsvInventoryStore(Path folder) {
        this.productsFile = folder.resolve("products.csv");
        this.historyFile = folder.resolve("history.csv");
    }

    public Path folder() {
        return productsFile.getParent();
    }

    public boolean exists() {
        return Files.exists(productsFile);
    }

    public void load(Inventory inventory) throws IOException {
        List<Product> products = new ArrayList<>();
        List<StockMovement> history = new ArrayList<>();

        if (Files.exists(productsFile)) {
            List<String> lines = Files.readAllLines(productsFile, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                List<String> f = Csv.parseLine(lines.get(i));
                try {
                    products.add(new Product(f.get(0), f.get(1), f.get(2),
                            Integer.parseInt(f.get(3)), Integer.parseInt(f.get(4)), new BigDecimal(f.get(5))));
                } catch (RuntimeException e) {
                    throw new IOException("products.csv line " + (i + 1) + " couldn't be read: " + e.getMessage(), e);
                }
            }
        }

        if (Files.exists(historyFile)) {
            List<String> lines = Files.readAllLines(historyFile, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).isBlank()) continue;
                List<String> f = Csv.parseLine(lines.get(i));
                try {
                    history.add(new StockMovement(LocalDateTime.parse(f.get(0)), f.get(1),
                            StockMovement.Type.valueOf(f.get(2)), Integer.parseInt(f.get(3)),
                            Integer.parseInt(f.get(4)), f.size() > 5 ? f.get(5) : ""));
                } catch (RuntimeException e) {
                    throw new IOException("history.csv line " + (i + 1) + " couldn't be read: " + e.getMessage(), e);
                }
            }
        }

        inventory.restore(products, history);
    }

    public void save(Inventory inventory) throws IOException {
        Files.createDirectories(folder());

        List<String> productLines = new ArrayList<>();
        productLines.add(PRODUCTS_HEADER);
        for (Product p : inventory.all()) {
            productLines.add(Csv.joinLine(List.of(p.getSku(), p.getName(), p.getCategory(),
                    String.valueOf(p.getQuantity()), String.valueOf(p.getReorderLevel()), p.getUnitPrice().toPlainString())));
        }

        List<String> historyLines = new ArrayList<>();
        historyLines.add(HISTORY_HEADER);
        for (StockMovement m : inventory.history()) {
            historyLines.add(Csv.joinLine(List.of(m.time().toString(), m.sku(), m.type().name(),
                    String.valueOf(m.change()), String.valueOf(m.balanceAfter()), m.note())));
        }

        writeSafely(productsFile, productLines);
        writeSafely(historyFile, historyLines);
    }

    private static void writeSafely(Path target, List<String> lines) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
