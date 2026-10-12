package trace.ui;

import trace.BusinessRuleException;
import trace.Model.Option;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Shared layout helpers, colours and input parsing for all screens. */
public final class Ui {
    public static final Color NAVY = new Color(0x1F, 0x3A, 0x5F);
    public static final Color HOLD = new Color(0xFF, 0xF1, 0xC2);
    public static final Color RECALLED = new Color(0xF8, 0xD7, 0xD3);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 15);

    private Ui() {
    }

    /** A two-column form: label, field, label, field, ... */
    public static JPanel form(String title, Object... pairs) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(titled(title));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 8, 5, 8);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < pairs.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            p.add(new JLabel(String.valueOf(pairs[i])), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            p.add((Component) pairs[i + 1], c);
        }
        return p;
    }

    public static TitledBorder titled(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD1, 0xDA)), title);
        b.setTitleFont(TITLE_FONT);
        b.setTitleColor(NAVY);
        return b;
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        return f;
    }

    public static JTextField dateField(LocalDate d) {
        return new JTextField(d.toString(), 10);
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        return b;
    }

    public static JComboBox<Option> combo(List<Option> options) {
        JComboBox<Option> box = new JComboBox<Option>();
        reload(box, options);
        return box;
    }

    public static void reload(JComboBox<Option> box, List<Option> options) {
        box.removeAllItems();
        for (Option o : options) {
            box.addItem(o);
        }
    }

    public static String id(JComboBox<Option> box) {
        Object o = box.getSelectedItem();
        return o instanceof Option ? ((Option) o).id : null;
    }

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void fill(DefaultTableModel m, List<Object[]> rows) {
        m.setRowCount(0);
        for (Object[] r : rows) {
            for (int i = 0; i < r.length; i++) {
                if (r[i] instanceof Number) {
                    double d = ((Number) r[i]).doubleValue();
                    r[i] = d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
                }
            }
            m.addRow(r);
        }
    }

    /** A table whose rows are tinted when any cell says HOLD or RECALLED. */
    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(26);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                if (!selected) {
                    Color bg = Color.WHITE;
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        Object v = table.getValueAt(row, i);
                        if ("RECALLED".equals(v)) {
                            bg = RECALLED;
                        } else if ("HOLD".equals(v) && bg == Color.WHITE) {
                            bg = HOLD;
                        }
                    }
                    c.setBackground(bg);
                }
                return c;
            }
        });
        return t;
    }

    /** Relative column widths; the table still stretches to fill its space. */
    public static JTable widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
        return t;
    }

    public static JScrollPane scroll(JComponent c, String title) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(titled(title));
        s.setPreferredSize(new Dimension(400, 200));
        return s;
    }

    public static LocalDate date(JTextField f, String field) throws BusinessRuleException {
        try {
            return LocalDate.parse(f.getText().trim());
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException(field + " must be a date written as YYYY-MM-DD.");
        }
    }

    public static double number(JTextField f, String field) throws BusinessRuleException {
        try {
            return Double.parseDouble(f.getText().trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException(field + " must be a number.");
        }
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, Exception e) {
        if (e instanceof BusinessRuleException) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Business rule", JOptionPane.WARNING_MESSAGE);
        } else if (e instanceof SQLException) {
            JOptionPane.showMessageDialog(parent, "The database rejected the change:\n" + e.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, String.valueOf(e), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
