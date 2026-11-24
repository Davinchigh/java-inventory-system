package com.benedict.inventory.ui;

import com.benedict.inventory.model.Product;
import com.benedict.inventory.model.StockMovement;
import com.benedict.inventory.service.Inventory;
import com.benedict.inventory.storage.CsvInventoryStore;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Main window: a header, four summary cards, the stock table with search and filters,
 * and a side panel with stock value by category and the latest movements.
 */
public class InventoryApp extends JFrame {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, HH:mm");
    private static final NumberFormat AMOUNT = amountFormat();

    private final Inventory inventory;
    private final CsvInventoryStore store;
    private final Theme theme = Theme.current();

    private final ProductTableModel tableModel = new ProductTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(24);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JCheckBox lowStockOnly = new JCheckBox("Low stock only");

    private final JLabel productsLabel = statValue();
    private final JLabel productsNote = statNote();
    private final JLabel unitsLabel = statValue();
    private final JLabel unitsNote = statNote();
    private final JLabel valueLabel = statValue();
    private final JLabel valueNote = statNote();
    private final JLabel lowStockLabel = statValue();
    private final JLabel lowStockNote = statNote();
    private final JLabel statusLabel = new JLabel(" ");

    private final Ui.CategoryChart chart = new Ui.CategoryChart(InventoryApp::money);
    private final JPanel recent = new JPanel();

    private boolean refreshing;
    private JPanel filters;

    private final JButton editButton = new Ui.FlatButton("Edit", false);
    private final JButton stockInButton = new Ui.FlatButton("Stock in", false);
    private final JButton stockOutButton = new Ui.FlatButton("Stock out", false);
    private final JButton removeButton = new Ui.FlatButton("Remove", false);

    public InventoryApp(Inventory inventory, CsvInventoryStore store) {
        super("Inventory");
        this.inventory = inventory;
        this.store = store;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 620));
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(theme.background);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);
        main.add(buildTop(), BorderLayout.NORTH);
        main.add(buildTable(), BorderLayout.CENTER);
        body.add(main, BorderLayout.CENTER);
        body.add(buildSide(), BorderLayout.EAST);
        root.add(body, BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        bindKeys(root);

        refresh();
        updateButtons();
        pack();
        setSize(1320, 740);
        setLocationRelativeTo(null);
    }

    // ---- Layout ----

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.add(new Ui.Logo());
        JPanel words = new JPanel(new GridLayout(2, 1));
        words.setOpaque(false);
        words.add(Ui.label("Inventory", theme.text, Font.BOLD, 17f));
        words.add(Ui.label("Stock, reorders and history for a small shop", theme.muted, Font.PLAIN, 12.5f));
        brand.add(words);

        JButton reorder = new Ui.FlatButton("Reorder report", false);
        reorder.setToolTipText("Save a CSV of what to reorder and what it will cost (Ctrl+R)");
        reorder.addActionListener(e -> exportReorderPlan());
        JButton themeButton = new Ui.FlatButton(theme.dark ? "Light mode" : "Dark mode", false);
        themeButton.addActionListener(e -> switchTheme());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(reorder);
        right.add(themeButton);

        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildTop() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(stat("Products", productsLabel, productsNote, null));
        stats.add(stat("Units in stock", unitsLabel, unitsNote, null));
        stats.add(stat("Stock value", valueLabel, valueNote, null));
        stats.add(stat("Need reordering", lowStockLabel, lowStockNote, theme.lowText));

        JButton addButton = new Ui.FlatButton("+ Add product", true);
        JButton historyButton = new Ui.FlatButton("History", false);
        addButton.setToolTipText("Add a product (Ctrl+N)");
        stockInButton.setToolTipText("Record stock received (Ctrl+I)");
        stockOutButton.setToolTipText("Record stock sold or used (Ctrl+O)");
        removeButton.setToolTipText("Remove the selected product (Delete)");
        addButton.addActionListener(e -> addProduct());
        editButton.addActionListener(e -> editProduct());
        stockInButton.addActionListener(e -> changeStock(true));
        stockOutButton.addActionListener(e -> changeStock(false));
        removeButton.addActionListener(e -> removeProduct());
        historyButton.addActionListener(e -> showHistory());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        for (JButton b : new JButton[]{addButton, editButton, stockInButton, stockOutButton, removeButton, historyButton}) actions.add(b);

        searchField.putClientProperty("JTextField.placeholderText", "Search SKU, name or category");
        searchField.setToolTipText("Search by SKU, name or category (Ctrl+F)");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.line), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        categoryFilter.addActionListener(e -> refresh());
        lowStockOnly.setOpaque(false);
        lowStockOnly.addActionListener(e -> refresh());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(Ui.label("Search", theme.muted, Font.PLAIN, 13f));
        filters.add(searchField);
        filters.add(categoryFilter);
        filters.add(lowStockOnly);

        this.filters = filters;

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(stats, BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);
        return top;
    }

    private JComponent buildTable() {
        table.setRowHeight(38);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(theme.surface);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) editProduct();
            }
        });

        Ui.Cell cell = new Ui.Cell(InventoryApp::money);
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(cell);
        table.getColumnModel().getColumn(3).setCellRenderer(new Ui.StockBar());
        table.getColumnModel().getColumn(7).setCellRenderer(new Ui.StatusPill());
        int[] widths = {84, 190, 96, 104, 84, 108, 118, 118};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(theme.surface);
        Ui.Card card = new Ui.Card(new BorderLayout(0, 8), 16);
        card.setBorder(BorderFactory.createEmptyBorder(12, 6, 9, 6));
        filters.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        card.add(filters, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildSide() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(290, 0));

        Ui.Card value = new Ui.Card(new BorderLayout(0, 12), 16);
        value.add(Ui.label("Stock value by category", theme.text, Font.BOLD, 14f), BorderLayout.NORTH);
        value.add(chart, BorderLayout.CENTER);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        Ui.Card moves = new Ui.Card(new BorderLayout(0, 10), 16);
        moves.add(Ui.label("Latest movements", theme.text, Font.BOLD, 14f), BorderLayout.NORTH);
        recent.setOpaque(false);
        recent.setLayout(new BoxLayout(recent, BoxLayout.Y_AXIS));
        moves.add(recent, BorderLayout.CENTER);
        moves.setAlignmentX(Component.LEFT_ALIGNMENT);

        side.add(value);
        side.add(Box.createVerticalStrut(12));
        side.add(moves);
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 20, 12, 20));
        statusLabel.setForeground(theme.text);
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel saved = Ui.label("Saved automatically to " + store.folder(), theme.muted, Font.PLAIN, 12f);
        bar.add(saved, BorderLayout.EAST);
        return bar;
    }

    private void bindKeys(JComponent root) {
        int menu = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_N, menu), "add", this::addProduct);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), "find", searchField::requestFocusInWindow);
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_I, menu), "in", () -> changeStock(true));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_O, menu), "out", () -> changeStock(false));
        bind(root, KeyStroke.getKeyStroke(KeyEvent.VK_R, menu), "reorder", this::exportReorderPlan);
        bind(table, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove", this::removeProduct);
        bind(table, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "edit", this::editProduct);
    }

    private static void bind(JComponent c, KeyStroke key, String name, Runnable action) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        c.getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
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

    private void exportReorderPlan() {
        List<Inventory.ReorderLine> plan = inventory.reorderPlan();
        if (plan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing needs reordering right now.", "Reorder report", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        BigDecimal total = plan.stream().map(Inventory.ReorderLine::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        JFileChooser chooser = new JFileChooser(store.folder().toFile());
        chooser.setDialogTitle("Save reorder report");
        chooser.setSelectedFile(new File("reorder-" + java.time.LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            CsvInventoryStore.writeReorderPlan(chooser.getSelectedFile().toPath(), plan);
            statusLabel.setText("Saved a reorder report: " + plan.size() + " products, about " + money(total) + ".");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Couldn't save the report:\n" + e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void switchTheme() {
        Theme.toggle();
        Rectangle bounds = getBounds();
        InventoryApp fresh = new InventoryApp(inventory, store);
        fresh.setBounds(bounds);
        fresh.setVisible(true);
        dispose();
    }

    private void showHistory() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"When", "SKU", "What", "Change", "Balance", "Note"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        DateTimeFormatter when = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
        List<StockMovement> history = inventory.history();
        for (int i = history.size() - 1; i >= 0; i--) {
            StockMovement m = history.get(i);
            model.addRow(new Object[]{m.time().format(when), m.sku(), m.type().label(),
                    (m.change() > 0 ? "+" : "") + m.change(), m.balanceAfter(), m.note()});
        }
        JTable historyTable = new JTable(model);
        historyTable.setRowHeight(30);
        historyTable.setShowGrid(false);
        historyTable.setIntercellSpacing(new Dimension(0, 0));
        historyTable.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        Ui.Cell cell = new Ui.Cell(InventoryApp::money);
        for (int i = 0; i < historyTable.getColumnCount(); i++) historyTable.getColumnModel().getColumn(i).setCellRenderer(cell);

        JDialog dialog = new JDialog(this, "Stock history", true);
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.getViewport().setBackground(theme.surface);
        dialog.add(scroll);
        dialog.setSize(780, 440);
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

        List<Product> all = inventory.all();
        productsLabel.setText(String.valueOf(all.size()));
        productsNote.setText("in " + categories.size() + (categories.size() == 1 ? " category" : " categories"));
        unitsLabel.setText(String.format("%,d", inventory.totalUnits()));
        long empty = all.stream().filter(p -> p.getQuantity() == 0).count();
        unitsNote.setText(empty == 0 ? "nothing out of stock" : empty + " product" + (empty == 1 ? "" : "s") + " out of stock");
        valueLabel.setText(money(inventory.totalValue()));
        valueNote.setText("at current unit prices");
        List<Inventory.ReorderLine> plan = inventory.reorderPlan();
        lowStockLabel.setText(String.valueOf(plan.size()));
        BigDecimal cost = plan.stream().map(Inventory.ReorderLine::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        lowStockNote.setText(plan.isEmpty() ? "all stocked up" : "about " + money(cost) + " to restock");
        lowStockLabel.setForeground(plan.isEmpty() ? theme.text : theme.lowText);

        chart.setData(inventory.valueByCategory());
        refreshRecent();
    }

    private void refreshRecent() {
        recent.removeAll();
        List<StockMovement> history = inventory.history();
        int shown = 0;
        for (int i = history.size() - 1; i >= 0 && shown < 6; i--, shown++) {
            StockMovement m = history.get(i);
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            String name = inventory.find(m.sku()).map(Product::getName).orElse(m.sku());
            JPanel left = new JPanel(new GridLayout(2, 1));
            left.setOpaque(false);
            left.add(Ui.label(shorten(name, 26), theme.text, Font.PLAIN, 12.5f));
            left.add(Ui.label(m.type().label() + ", " + m.time().format(WHEN), theme.muted, Font.PLAIN, 11.5f));
            String change = (m.change() > 0 ? "+" : "") + m.change();
            JLabel delta = Ui.label(change, m.change() > 0 ? theme.okText : m.change() < 0 ? theme.outText : theme.muted, Font.BOLD, 13f);
            delta.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(left, BorderLayout.CENTER);
            row.add(delta, BorderLayout.EAST);
            recent.add(row);
        }
        if (shown == 0) recent.add(Ui.label("No movements yet.", theme.muted, Font.PLAIN, 12.5f));
        recent.revalidate();
        recent.repaint();
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

    private Ui.Card stat(String title, JLabel value, JLabel note, Color accent) {
        Ui.Card card = new Ui.Card(new BorderLayout(0, 2), 16);
        card.add(Ui.label(title, theme.muted, Font.PLAIN, 12.5f), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(note, BorderLayout.SOUTH);
        if (accent != null) value.setForeground(accent);
        return card;
    }

    private JLabel statValue() {
        return Ui.label("0", Theme.current().text, Font.BOLD, 24f);
    }

    private JLabel statNote() {
        return Ui.label(" ", Theme.current().muted, Font.PLAIN, 12f);
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    static String money(BigDecimal amount) {
        return "GH₵ " + AMOUNT.format(amount);
    }

    private static NumberFormat amountFormat() {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.UK);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return f;
    }

    /** Quiet, small-caps column headers on the table's tinted header row. */
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(t, value == null ? "" : value.toString().toUpperCase(Locale.ROOT), false, false, row, column);
            Theme theme = Theme.current();
            setBackground(theme.surfaceAlt);
            setForeground(theme.muted);
            setFont(Theme.font(Font.BOLD, 11f));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, theme.line), BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            Class<?> type = t.getModel().getColumnClass(t.convertColumnIndexToModel(column));
            setHorizontalAlignment(Number.class.isAssignableFrom(type) || type == BigDecimal.class ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }
    }
}
