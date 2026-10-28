package com.benedict.inventory.ui;

import com.benedict.inventory.model.Product;
import com.benedict.inventory.model.StockMovement;
import com.benedict.inventory.service.Inventory;
import com.benedict.inventory.storage.CsvInventoryStore;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Main window: the stock table with search, filters, actions and a running summary. */
public class InventoryApp extends JFrame {

    private static final Color LOW_STOCK_BG = new Color(0xFFF4E5);
    private static final Color OUT_OF_STOCK_BG = new Color(0xFDE8E8);
    private static final NumberFormat MONEY = moneyFormat();

    private final Inventory inventory;
    private final CsvInventoryStore store;

    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JCheckBox lowStockOnly = new JCheckBox("Low stock only");

    private final JLabel productsLabel = statLabel();
    private final JLabel unitsLabel = statLabel();
    private final JLabel valueLabel = statLabel();
    private final JLabel lowStockLabel = statLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private boolean refreshing;

    private final JButton editButton = new JButton("Edit");
    private final JButton stockInButton = new JButton("Stock in");
    private final JButton stockOutButton = new JButton("Stock out");
    private final JButton removeButton = new JButton("Remove");

    public InventoryApp(Inventory inventory, CsvInventoryStore store) {
        super("Inventory");
        this.inventory = inventory;
        this.store = store;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 560));
        setLayout(new BorderLayout());

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        refresh();
        updateButtons();
        pack();
        setSize(1120, 640);
        setLocationRelativeTo(null);
    }

    // ---- Layout ----

    private JPanel buildTop() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.add(stat("Products", productsLabel));
        stats.add(stat("Units in stock", unitsLabel));
        stats.add(stat("Stock value", valueLabel));
        stats.add(stat("Need reordering", lowStockLabel));

        JButton addButton = new JButton("Add product");
        JButton historyButton = new JButton("History");
        addButton.addActionListener(e -> addProduct());
        editButton.addActionListener(e -> editProduct());
        stockInButton.addActionListener(e -> changeStock(true));
        stockOutButton.addActionListener(e -> changeStock(false));
        removeButton.addActionListener(e -> removeProduct());
        historyButton.addActionListener(e -> showHistory());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.add(addButton);
        actions.add(editButton);
        actions.add(stockInButton);
        actions.add(stockOutButton);
        actions.add(removeButton);
        actions.add(historyButton);

        searchField.putClientProperty("JTextField.placeholderText", "Search SKU, name or category");
        searchField.setToolTipText("Search by SKU, name or category");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        categoryFilter.addActionListener(e -> refresh());
        lowStockOnly.addActionListener(e -> refresh());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filters.add(new JLabel("Search"));
        filters.add(searchField);
        filters.add(categoryFilter);
        filters.add(lowStockOnly);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.add(actions, BorderLayout.WEST);
        toolbar.add(filters, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 16));
        top.add(stats, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.SOUTH);
        return top;
    }

    private JScrollPane buildTable() {
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) editProduct();
            }
        });

        StockRenderer renderer = new StockRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        int[] widths = {90, 240, 130, 80, 90, 100, 110, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 16, 0, 16),
                BorderFactory.createLineBorder(new Color(0xDDDDDD))));
        return scroll;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 10, 16));
        statusLabel.setForeground(new Color(0x555555));
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel saved = new JLabel("Saved automatically to " + store.folder());
        saved.setForeground(new Color(0x777777));
        bar.add(saved, BorderLayout.EAST);
        return bar;
    }

    // ---- Actions ----

    private void addProduct() {
        ProductDialog.showAdd(this, inventory.categories()).ifPresent(v -> attempt(() -> {
            inventory.addProduct(new Product(v.sku(), v.name(), v.category(), v.quantity(), v.reorderLevel(), v.unitPrice()));
            return "Added " + v.name() + ".";
        }));
    }

    private void editProduct() {
        Product p = selected();
        if (p == null) return;
        ProductDialog.showEdit(this, inventory.categories(), p).ifPresent(v -> attempt(() -> {
            inventory.updateDetails(p.getSku(), v.name(), v.category(), v.reorderLevel(), v.unitPrice());
            return "Updated " + v.name() + ".";
        }));
    }

    private void changeStock(boolean incoming) {
        Product p = selected();
        if (p == null) return;

        JSpinner amount = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JTextField note = new JTextField(incoming ? "Delivery" : "Sale", 20);
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel(p.getName() + ", currently " + p.getQuantity() + " in stock"));
        form.add(new JLabel(incoming ? "Quantity received" : "Quantity issued"));
        form.add(amount);
        form.add(new JLabel("Note"));
        form.add(note);

        int choice = JOptionPane.showConfirmDialog(this, form, incoming ? "Stock in" : "Stock out",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        int qty = (Integer) amount.getValue();
        attempt(() -> {
            if (incoming) {
                inventory.stockIn(p.getSku(), qty, note.getText());
                return "Received " + qty + " × " + p.getName() + ".";
            }
            inventory.stockOut(p.getSku(), qty, note.getText());
            return p.isLowStock()
                    ? "Issued " + qty + " × " + p.getName() + ". Stock is now low, time to reorder."
                    : "Issued " + qty + " × " + p.getName() + ".";
        });
    }

    private void removeProduct() {
        Product p = selected();
        if (p == null) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove " + p.getName() + " (" + p.getSku() + ") from the inventory?\nIts history is kept.",
                "Remove product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            attempt(() -> {
                inventory.removeProduct(p.getSku());
                return "Removed " + p.getName() + ".";
            });
        }
    }

    private void showHistory() {
        DateTimeFormatter when = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
        DefaultTableModel model = new DefaultTableModel(new String[]{"When", "SKU", "What", "Change", "Balance", "Note"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        List<StockMovement> history = inventory.history();
        for (int i = history.size() - 1; i >= 0; i--) {
            StockMovement m = history.get(i);
            model.addRow(new Object[]{m.time().format(when), m.sku(), m.type().label(),
                    (m.change() > 0 ? "+" : "") + m.change(), m.balanceAfter(), m.note()});
        }
        JTable historyTable = new JTable(model);
        historyTable.setRowHeight(26);

        JDialog dialog = new JDialog(this, "Stock history", true);
        dialog.add(new JScrollPane(historyTable));
        dialog.setSize(760, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /** Runs a change, saves, refreshes the view and reports the result or the error. */
    private void attempt(Action action) {
        try {
            String message = action.run();
            store.save(inventory);
            refresh();
            statusLabel.setText(message);
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Can't do that", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            refresh();
            JOptionPane.showMessageDialog(this, "The change was made but couldn't be saved:\n" + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    @FunctionalInterface
    private interface Action {
        String run() throws IOException;
    }

    // ---- View updates ----

    private void refresh() {
        if (refreshing) return; // changing the category list fires events; ignore those
        refreshing = true;
        try {
            refreshView();
        } finally {
            refreshing = false;
        }
    }

    private void refreshView() {
        String keepSku = selected() == null ? null : selected().getSku();

        Object chosen = categoryFilter.getSelectedItem();
        List<String> categories = inventory.categories();
        categoryFilter.removeAllItems();
        categoryFilter.addItem("All categories");
        categories.forEach(categoryFilter::addItem);
        categoryFilter.setSelectedItem(chosen != null && categories.contains(chosen) ? chosen : "All categories");

        String category = Objects.toString(categoryFilter.getSelectedItem(), "All categories");
        List<Product> rows = inventory.search(searchField.getText()).stream()
                .filter(p -> category.equals("All categories") || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> !lowStockOnly.isSelected() || p.isLowStock())
                .toList();
        tableModel.setRows(rows);

        if (keepSku != null) {
            for (int i = 0; i < table.getRowCount(); i++) {
                if (tableModel.rowAt(table.convertRowIndexToModel(i)).getSku().equals(keepSku)) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }

        productsLabel.setText(String.valueOf(inventory.all().size()));
        unitsLabel.setText(String.format("%,d", inventory.totalUnits()));
        valueLabel.setText(MONEY.format(inventory.totalValue()));
        int low = inventory.lowStock().size();
        lowStockLabel.setText(String.valueOf(low));
        lowStockLabel.setForeground(low > 0 ? new Color(0xB45309) : Color.BLACK);
    }

    private void updateButtons() {
        boolean hasSelection = table.getSelectedRow() >= 0;
        editButton.setEnabled(hasSelection);
        stockInButton.setEnabled(hasSelection);
        stockOutButton.setEnabled(hasSelection);
        removeButton.setEnabled(hasSelection);
    }

    private Product selected() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : tableModel.rowAt(table.convertRowIndexToModel(viewRow));
    }

    // ---- Small helpers ----

    private static JPanel stat(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel label = new JLabel(title);
        label.setForeground(new Color(0x555555));
        panel.add(label, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel statLabel() {
        JLabel label = new JLabel("0");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
        return label;
    }

    private static NumberFormat moneyFormat() {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.UK);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return new NumberFormat() {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                return toAppendTo.append("GH₵ ").append(f.format(number));
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                return toAppendTo.append("GH₵ ").append(f.format(number));
            }

            @Override
            public Number parse(String source, java.text.ParsePosition parsePosition) {
                return f.parse(source.replace("GH₵ ", ""), parsePosition);
            }
        };
    }

    /** Highlights low and out of stock rows, right-aligns numbers and formats money. */
    private class StockRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
            Object shown = value instanceof BigDecimal money ? MONEY.format(money) : value;
            Component c = super.getTableCellRendererComponent(t, shown, selected, focus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);

            Product p = tableModel.rowAt(t.convertRowIndexToModel(row));
            if (!selected) {
                c.setBackground(p.getQuantity() == 0 ? OUT_OF_STOCK_BG : p.isLowStock() ? LOW_STOCK_BG : Color.WHITE);
            }
            c.setFont(c.getFont().deriveFont(column == 7 && p.isLowStock() ? Font.BOLD : Font.PLAIN));
            return c;
        }
    }
}
