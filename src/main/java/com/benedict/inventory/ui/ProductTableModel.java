package com.benedict.inventory.ui;

import com.benedict.inventory.model.Product;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shows a list of products as table rows. */
class ProductTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"SKU", "Name", "Category", "In stock", "Reorder at", "Unit price", "Stock value", "Status"};

    private List<Product> rows = new ArrayList<>();

    void setRows(List<Product> products) {
        this.rows = new ArrayList<>(products);
        fireTableDataChanged();
    }

    Product rowAt(int row) {
        return rows.get(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 3, 4 -> Integer.class;
            case 5, 6 -> BigDecimal.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int column) {
        Product p = rows.get(row);
        return switch (column) {
            case 0 -> p.getSku();
            case 1 -> p.getName();
            case 2 -> p.getCategory();
            case 3 -> p.getQuantity();
            case 4 -> p.getReorderLevel();
            case 5 -> p.getUnitPrice();
            case 6 -> p.stockValue();
            case 7 -> p.getQuantity() == 0 ? "Out of stock" : p.isLowStock() ? "Low stock" : "OK";
            default -> "";
        };
    }
}
