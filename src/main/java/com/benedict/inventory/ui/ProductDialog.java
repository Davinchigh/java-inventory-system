package com.benedict.inventory.ui;

import com.benedict.inventory.model.Product;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Form for adding a new product or editing an existing one's details. */
class ProductDialog extends JDialog {

    /** What the user entered. Quantity is only used when adding. */
    record Values(String sku, String name, String category, int quantity, int reorderLevel, BigDecimal unitPrice) {
    }

    private final JTextField skuField = new JTextField(14);
    private final JTextField nameField = new JTextField(24);
    private final JComboBox<String> categoryField;
    private final JSpinner quantityField = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JSpinner reorderField = new JSpinner(new SpinnerNumberModel(5, 0, 1_000_000, 1));
    private final JTextField priceField = new JTextField(10);

    private Values result;

    private ProductDialog(JFrame owner, String title, List<String> categories, Product existing) {
        super(owner, title, true);
        categoryField = new JComboBox<>(categories.toArray(String[]::new));
        categoryField.setEditable(true);

        if (existing != null) {
            skuField.setText(existing.getSku());
            skuField.setEditable(false);
            nameField.setText(existing.getName());
            categoryField.setSelectedItem(existing.getCategory());
            quantityField.setValue(existing.getQuantity());
            quantityField.setEnabled(false);
            quantityField.setToolTipText("Use Stock in or Stock out to change quantity");
            reorderField.setValue(existing.getReorderLevel());
            priceField.setText(existing.getUnitPrice().toPlainString());
        } else {
            categoryField.setSelectedItem("");
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        int row = 0;
        addRow(form, row++, "SKU", skuField);
        addRow(form, row++, "Name", nameField);
        addRow(form, row++, "Category", categoryField);
        addRow(form, row++, existing == null ? "Opening stock" : "In stock", quantityField);
        addRow(form, row++, "Reorder at", reorderField);
        addRow(form, row, "Unit price (GH₵)", priceField);

        JButton save = new JButton(existing == null ? "Add product" : "Save changes");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttons.add(cancel);
        buttons.add(save);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    static Optional<Values> showAdd(JFrame owner, List<String> categories) {
        ProductDialog d = new ProductDialog(owner, "Add product", categories, null);
        d.setVisible(true);
        return Optional.ofNullable(d.result);
    }

    static Optional<Values> showEdit(JFrame owner, List<String> categories, Product product) {
        ProductDialog d = new ProductDialog(owner, "Edit " + product.getName(), categories, product);
        d.setVisible(true);
        return Optional.ofNullable(d.result);
    }

    private void submit() {
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        Object category = categoryField.getEditor().getItem();
        String priceText = priceField.getText().trim().replace(",", "");

        if (sku.isEmpty() || name.isEmpty()) {
            error("Enter a SKU and a name.");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceText.isEmpty() ? "0" : priceText);
            if (price.signum() < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            error("Enter the unit price as a number, for example 12.50.");
            return;
        }

        result = new Values(sku, name, category == null ? "" : category.toString(),
                (Integer) quantityField.getValue(), (Integer) reorderField.getValue(), price);
        dispose();
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Check the form", JOptionPane.WARNING_MESSAGE);
    }

    private static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 0, 4, 12);
        c.anchor = GridBagConstraints.LINE_END;
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
    }
}
