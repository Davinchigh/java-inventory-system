package com.benedict.inventory;

import com.benedict.inventory.model.Product;
import com.benedict.inventory.service.Inventory;
import com.benedict.inventory.storage.CsvInventoryStore;
import com.benedict.inventory.ui.InventoryApp;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Starts the app. Data is kept in ./data next to where you run it,
 * or in the folder passed as the first argument.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path dataFolder = Path.of(args.length > 0 ? args[0] : "data");
        Inventory inventory = new Inventory();
        CsvInventoryStore store = new CsvInventoryStore(dataFolder);

        try {
            if (store.exists()) {
                store.load(inventory);
            } else {
                addSampleProducts(inventory);
                store.save(inventory);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Couldn't open the saved inventory:\n" + e.getMessage(),
                    "Inventory", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            useSystemLook();
            new InventoryApp(inventory, store).setVisible(true);
        });
    }

    /** A small stationery shop, so the app isn't empty the first time it opens. */
    static void addSampleProducts(Inventory inventory) {
        Object[][] samples = {
                {"STA-001", "A4 exercise book, 80 pages", "Stationery", 140, 40, "6.50"},
                {"STA-002", "Blue ballpoint pens (pack of 10)", "Stationery", 35, 20, "18.00"},
                {"STA-003", "Mathematical set", "Stationery", 12, 15, "25.00"},
                {"STA-004", "Scientific calculator", "Electronics", 8, 5, "185.00"},
                {"ELE-001", "USB flash drive 32GB", "Electronics", 22, 10, "75.00"},
                {"ELE-002", "Extension cord, 4 way", "Electronics", 0, 4, "95.00"},
                {"PRT-001", "A4 printing paper (ream)", "Printing", 30, 12, "65.00"},
                {"PRT-002", "Black toner cartridge", "Printing", 3, 3, "420.00"},
        };
        for (Object[] s : samples) {
            inventory.addProduct(new Product((String) s[0], (String) s[1], (String) s[2],
                    (Integer) s[3], (Integer) s[4], new BigDecimal((String) s[5])));
        }
    }

    private static void useSystemLook() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The default Swing look is fine if the system one isn't available.
        }
    }
}
