package com.benedict.inventory.ui;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.util.prefs.Preferences;

/**
 * The app's two colour schemes and the fonts. Every custom component reads its colours from
 * {@link #current()}, so switching theme is a matter of rebuilding the window.
 */
public final class Theme {

    public final boolean dark;
    public final Color background, surface, surfaceAlt, line, text, muted, primary, primaryText;
    public final Color okText, okFill, lowText, lowFill, outText, outFill, selection;

    private Theme(boolean dark) {
        this.dark = dark;
        if (dark) {
            background = new Color(0x0E1117);
            surface = new Color(0x161B22);
            surfaceAlt = new Color(0x1B212A);
            line = new Color(0x2A313C);
            text = new Color(0xE6EAF0);
            muted = new Color(0x9AA4B2);
            primary = new Color(0x8B93FF);
            primaryText = new Color(0x0E1117);
            okText = new Color(0x6BD49A);
            okFill = new Color(0x123222);
            lowText = new Color(0xF5B14C);
            lowFill = new Color(0x3A2A12);
            outText = new Color(0xFF8A80);
            outFill = new Color(0x3A1714);
            selection = new Color(0x262B4D);
        } else {
            background = new Color(0xF4F6F9);
            surface = Color.WHITE;
            surfaceAlt = new Color(0xF8F9FB);
            line = new Color(0xE3E7EC);
            text = new Color(0x121826);
            muted = new Color(0x5B6576);
            primary = new Color(0x4F46E5);
            primaryText = Color.WHITE;
            okText = new Color(0x067647);
            okFill = new Color(0xE7F6EC);
            lowText = new Color(0xB45309);
            lowFill = new Color(0xFFF4E5);
            outText = new Color(0xB42318);
            outFill = new Color(0xFDECEC);
            selection = new Color(0xEEF0FF);
        }
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static Theme current = new Theme(PREFS.getBoolean("dark", false));

    public static Theme current() {
        return current;
    }

    public static void toggle() {
        current = new Theme(!current.dark);
        PREFS.putBoolean("dark", current.dark);
        apply();
    }

    /** For screenshots and tests: pick a scheme without touching the saved preference. */
    public static void force(boolean dark) {
        current = new Theme(dark);
        apply();
    }

    public static Font font(int style, float size) {
        return new Font("SansSerif", style, 13).deriveFont(style, size);
    }

    /** Puts the colours into Swing's defaults so dialogs, menus and scroll bars match. */
    public static void apply() {
        Theme t = current;
        Font base = font(Font.PLAIN, 13f);
        for (Object key : UIManager.getLookAndFeelDefaults().keySet().toArray()) {
            if (key instanceof String k && k.endsWith(".font")) UIManager.put(k, new FontUIResource(base));
        }
        String[] panels = {"Panel.background", "OptionPane.background", "Viewport.background", "ScrollPane.background",
                "SplitPane.background", "CheckBox.background", "Label.background", "Spinner.background"};
        for (String k : panels) UIManager.put(k, new ColorUIResource(t.background));
        String[] fields = {"TextField.background", "FormattedTextField.background", "ComboBox.background", "List.background",
                "Table.background", "TextArea.background"};
        for (String k : fields) UIManager.put(k, new ColorUIResource(t.surface));
        String[] texts = {"Label.foreground", "CheckBox.foreground", "TextField.foreground", "FormattedTextField.foreground",
                "ComboBox.foreground", "List.foreground", "Table.foreground", "OptionPane.messageForeground", "TextArea.foreground",
                "TitledBorder.titleColor", "Button.foreground"};
        for (String k : texts) UIManager.put(k, new ColorUIResource(t.text));
        UIManager.put("TextField.caretForeground", new ColorUIResource(t.text));
        UIManager.put("FormattedTextField.caretForeground", new ColorUIResource(t.text));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(t.selection));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(t.text));
        UIManager.put("List.selectionBackground", new ColorUIResource(t.selection));
        UIManager.put("List.selectionForeground", new ColorUIResource(t.text));
        UIManager.put("Table.selectionBackground", new ColorUIResource(t.selection));
        UIManager.put("Table.selectionForeground", new ColorUIResource(t.text));
        UIManager.put("Table.gridColor", new ColorUIResource(t.line));
        UIManager.put("TableHeader.background", new ColorUIResource(t.surfaceAlt));
        UIManager.put("TableHeader.foreground", new ColorUIResource(t.muted));
        UIManager.put("Button.background", new ColorUIResource(t.surface));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(t.line));
        UIManager.put("ScrollBar.track", new ColorUIResource(t.background));
        UIManager.put("ToolTip.background", new ColorUIResource(t.surface));
        UIManager.put("ToolTip.foreground", new ColorUIResource(t.text));
    }
}
