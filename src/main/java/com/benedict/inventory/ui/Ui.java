package com.benedict.inventory.ui;

import com.benedict.inventory.model.Product;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

/** Small hand-drawn Swing components, so the app looks the same on every system. */
final class Ui {

    private Ui() {
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static JLabel label(String text, Color color, int style, float size) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(Theme.font(style, size));
        return l;
    }

    /** A rounded surface with a hairline border. */
    static class Card extends JPanel {
        private final int radius;

        Card(LayoutManager layout, int radius) {
            super(layout);
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme t = Theme.current();
            Graphics2D g2 = smooth(g);
            if (!t.dark) {
                g2.setColor(new Color(18, 24, 38, 10));
                g2.fill(new RoundRectangle2D.Float(0, 2, getWidth() - 1, getHeight() - 2, radius, radius));
            }
            g2.setColor(t.surface);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 3, radius, radius));
            g2.setColor(t.line);
            g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 3, radius, radius));
            g2.dispose();
        }
    }

    /** A flat rounded button: filled for the main action, outlined for the rest. */
    static class FlatButton extends JButton {
        private final boolean primary;
        private boolean hover;

        FlatButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(Theme.font(Font.BOLD, 13f));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme t = Theme.current();
            Graphics2D g2 = smooth(g);
            float w = getWidth() - 1, h = getHeight() - 1;
            boolean on = isEnabled();
            if (primary) {
                Color fill = on ? (hover ? t.primary.brighter() : t.primary) : t.line;
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                setForeground(on ? t.primaryText : t.muted);
            } else {
                g2.setColor(hover && on ? t.surfaceAlt : t.surface);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                g2.setColor(hover && on ? t.primary : t.line);
                g2.draw(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                setForeground(on ? t.text : t.muted);
            }
            if (isFocusOwner()) {
                g2.setColor(t.primary);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** The app's mark: a box with a rising stock line, in the primary colour. */
    static class Logo extends JComponent {
        Logo() {
            setPreferredSize(new Dimension(34, 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme t = Theme.current();
            Graphics2D g2 = smooth(g);
            g2.setColor(t.primary);
            g2.fill(new RoundRectangle2D.Float(0, 0, 33, 33, 10, 10));
            g2.setColor(t.primaryText);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect(8, 12, 17, 13, 3, 3);
            g2.drawLine(8, 17, 25, 17);
            g2.drawLine(13, 8, 16, 11);
            g2.drawLine(16, 11, 20, 7);
            g2.dispose();
        }
    }

    /** Draws text cells with padding and the theme's colours, and money as GH₵. */
    static class Cell extends DefaultTableCellRenderer {
        private final Function<BigDecimal, String> money;

        Cell(Function<BigDecimal, String> money) {
            this.money = money;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            Object shown = value instanceof BigDecimal m ? money.apply(m) : value;
            super.getTableCellRendererComponent(table, shown, selected, false, row, column);
            Theme t = Theme.current();
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBackground(selected ? t.selection : (row % 2 == 0 ? t.surface : t.surfaceAlt));
            setForeground(column == 0 ? t.muted : t.text);
            setFont(Theme.font(column == 1 ? Font.BOLD : Font.PLAIN, 13f));
            return this;
        }
    }

    /** The quantity with a small bar showing how full the shelf is against twice the reorder level. */
    static class StockBar extends JComponent implements javax.swing.table.TableCellRenderer {
        private Product product;
        private boolean selected;
        private int row;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean focus, int row, int column) {
            this.product = ((ProductTableModel) table.getModel()).rowAt(table.convertRowIndexToModel(row));
            this.selected = isSelected;
            this.row = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme t = Theme.current();
            Graphics2D g2 = smooth(g);
            g2.setColor(selected ? t.selection : (row % 2 == 0 ? t.surface : t.surfaceAlt));
            g2.fillRect(0, 0, getWidth(), getHeight());
            int q = product.getQuantity();
            String text = String.format("%,d", q);
            g2.setFont(Theme.font(Font.BOLD, 13f));
            FontMetrics fm = g2.getFontMetrics();
            int textX = getWidth() - 12 - fm.stringWidth(text);
            g2.setColor(t.text);
            g2.drawString(text, textX, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            int barW = Math.max(24, textX - 22), barH = 6, y = (getHeight() - barH) / 2;
            double full = Math.max(1, product.getReorderLevel() * 2.0);
            double share = Math.min(1, q / full);
            g2.setColor(t.line);
            g2.fill(new RoundRectangle2D.Float(12, y, barW, barH, barH, barH));
            g2.setColor(q == 0 ? t.outText : product.isLowStock() ? t.lowText : t.okText);
            g2.fill(new RoundRectangle2D.Float(12, y, (float) Math.max(q == 0 ? 0 : barH, barW * share), barH, barH, barH));
            g2.dispose();
        }
    }

    /** "OK", "Low stock" or "Out of stock" as a coloured pill. */
    static class StatusPill extends JComponent implements javax.swing.table.TableCellRenderer {
        private Product product;
        private boolean selected;
        private int row;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean focus, int row, int column) {
            this.product = ((ProductTableModel) table.getModel()).rowAt(table.convertRowIndexToModel(row));
            this.selected = isSelected;
            this.row = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme t = Theme.current();
            Graphics2D g2 = smooth(g);
            g2.setColor(selected ? t.selection : (row % 2 == 0 ? t.surface : t.surfaceAlt));
            g2.fillRect(0, 0, getWidth(), getHeight());
            boolean out = product.getQuantity() == 0, low = product.isLowStock();
            String text = out ? "Out of stock" : low ? "Low stock" : "In stock";
            g2.setFont(Theme.font(Font.BOLD, 12f));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 24, h = 22, x = 12, y = (getHeight() - h) / 2;
            g2.setColor(out ? t.outFill : low ? t.lowFill : t.okFill);
            g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
            Color ink = out ? t.outText : low ? t.lowText : t.okText;
            g2.setColor(ink);
            g2.fillOval(x + 9, y + h / 2 - 3, 6, 6);
            g2.drawString(text, x + 19, y + (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Horizontal bars of stock value per category. */
    static class CategoryChart extends JComponent {
        private Map<String, BigDecimal> data = Map.of();
        private final Function<BigDecimal, String> money;

        CategoryChart(Function<BigDecimal, String> money) {
            this.money = money;
            setOpaque(false);
        }

        void setData(Map<String, BigDecimal> data) {
            this.data = data;
            setPreferredSize(new Dimension(240, Math.max(1, data.size()) * 46));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme t = Theme.current();
            Graphics2D g2 = smooth(g);
            BigDecimal max = data.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
            if (max.signum() == 0) max = BigDecimal.ONE;
            Color[] palette = {t.primary, t.okText, t.lowText, t.outText, t.muted};
            int y = 0, i = 0, w = getWidth();
            for (Map.Entry<String, BigDecimal> e : data.entrySet()) {
                g2.setFont(Theme.font(Font.PLAIN, 12.5f));
                g2.setColor(t.text);
                g2.drawString(e.getKey(), 0, y + 14);
                String v = money.apply(e.getValue());
                g2.setColor(t.muted);
                g2.drawString(v, w - g2.getFontMetrics().stringWidth(v), y + 14);
                double share = e.getValue().doubleValue() / max.doubleValue();
                g2.setColor(t.line);
                g2.fill(new RoundRectangle2D.Float(0, y + 22, w, 8, 8, 8));
                g2.setColor(palette[i % palette.length]);
                g2.fill(new RoundRectangle2D.Float(0, y + 22, (float) Math.max(8, w * share), 8, 8, 8));
                y += 46;
                i++;
            }
            g2.dispose();
        }
    }
}
