package trace.ui;

import trace.TraceService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Screen 1: every lot with its source, status, stock left and expiry date. */
public class LotsPanel extends JPanel implements Refreshable {
    private final TraceService service;
    final JComboBox<String> typeFilter = new JComboBox<String>(new String[]{"All lots", "RAW", "INTERMEDIATE", "FINISHED"});
    final DefaultTableModel model = Ui.model("Lot", "Item", "Type", "Status", "Quantity", "Available", "Unit",
            "Made on", "Expires on", "Source");
    final JTable table = Ui.widths(Ui.table(model), 70, 200, 120, 100, 85, 85, 55, 105, 105, 230);

    public LotsPanel(TraceService service) {
        super(new BorderLayout(8, 8));
        this.service = service;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        top.add(new JLabel("Show:"));
        top.add(typeFilter);
        JButton refresh = Ui.button("Refresh");
        top.add(refresh);
        top.add(new JLabel("   Yellow rows are on HOLD, red rows are RECALLED."));
        add(top, BorderLayout.NORTH);
        add(Ui.scroll(table, "Lots and stock (from views LOT_STOCK and LOT_EXPIRY)"), BorderLayout.CENTER);
        refresh.addActionListener(e -> refresh());
        typeFilter.addActionListener(e -> refresh());
    }

    @Override
    public void refresh() {
        try {
            String t = typeFilter.getSelectedIndex() == 0 ? null : (String) typeFilter.getSelectedItem();
            Ui.fill(model, service.dao().lotOverview(t));
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
