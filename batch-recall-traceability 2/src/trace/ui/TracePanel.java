package trace.ui;

import trace.Model.Option;
import trace.Model.TraceRow;
import trace.TraceService;
import trace.TraceService.TraceResult;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** Screen 6 (QA officer): trace a lot both ways and raise a recall. */
public class TracePanel extends JPanel implements Refreshable {
    private final TraceService service;
    final JComboBox<Option> lot = new JComboBox<Option>();
    final JLabel summary = new JLabel("Pick a lot and press Trace.");
    final DefaultTableModel upModel = Ui.model("Lot", "Steps back", "Item", "Status", "Came from");
    final DefaultTableModel downModel = Ui.model("Lot", "Steps forward", "Item", "Status", "Made in");
    final DefaultTableModel shipModel = Ui.model("Shipment", "Date", "Customer", "City", "Phone", "Lot", "Quantity");
    final JTextField reason = Ui.field(24);
    final JComboBox<String> severity = new JComboBox<String>(new String[]{"HIGH", "MEDIUM", "LOW"});
    final JComboBox<Option> initiatedBy = new JComboBox<Option>();

    public TracePanel(TraceService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        top.add(new JLabel("Lot:"));
        top.add(lot);
        JButton traceButton = Ui.button("Trace");
        top.add(traceButton);
        summary.setFont(summary.getFont().deriveFont(Font.BOLD));
        top.add(summary);
        add(top, BorderLayout.NORTH);

        JPanel traces = new JPanel(new GridLayout(1, 2, 10, 0));
        traces.add(Ui.scroll(Ui.widths(Ui.table(upModel), 65, 85, 120, 85, 360), "Upstream: what went into this lot"));
        traces.add(Ui.scroll(Ui.widths(Ui.table(downModel), 65, 105, 200, 95, 170), "Downstream: what this lot went into"));

        JPanel recallForm = Ui.form("Raise a recall on this lot",
                "Reason", reason,
                "Severity", severity,
                "Initiated by", initiatedBy);
        JButton recall = Ui.button("Raise recall");
        JPanel recallBox = new JPanel(new BorderLayout());
        recallBox.add(recallForm, BorderLayout.NORTH);
        JPanel rb = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rb.add(recall);
        recallBox.add(rb, BorderLayout.CENTER);
        recallBox.setPreferredSize(new Dimension(470, 220));

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.add(Ui.scroll(Ui.widths(Ui.table(shipModel), 75, 100, 260, 90, 120, 60, 75), "Shipments to retrieve: customers to contact"), BorderLayout.CENTER);
        bottom.add(recallBox, BorderLayout.EAST);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 10));
        center.add(traces);
        center.add(bottom);
        add(center, BorderLayout.CENTER);

        traceButton.addActionListener(e -> runTrace());
        recall.addActionListener(e -> raiseRecall());
    }

    void runTrace() {
        try {
            TraceResult r = service.trace(Ui.id(lot));
            Ui.fill(upModel, rows(r.upstream));
            Ui.fill(downModel, rows(r.downstream));
            Ui.fill(shipModel, r.shipments);
            summary.setText(r.lot.lotId + " · " + r.lot.itemName + " · " + r.lot.status + "   →   "
                    + r.upstream.size() + " lots upstream, " + r.downstream.size() + " lots downstream, "
                    + r.shipments.size() + " shipment lines");
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    void raiseRecall() {
        try {
            String id = Ui.id(lot);
            String msg = service.raiseRecall(id, Ui.id(initiatedBy), reason.getText(),
                    (String) severity.getSelectedItem());
            reason.setText("");
            refresh();
            select(id);
            runTrace();
            Ui.info(this, msg);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    void select(String lotId) {
        for (int i = 0; i < lot.getItemCount(); i++) {
            if (lot.getItemAt(i).id.equals(lotId)) {
                lot.setSelectedIndex(i);
                return;
            }
        }
    }

    private static List<Object[]> rows(List<TraceRow> list) {
        List<Object[]> out = new ArrayList<Object[]>();
        for (TraceRow t : list) {
            out.add(new Object[]{t.lotId, t.steps, t.itemName, t.status, t.source});
        }
        return out;
    }

    @Override
    public void refresh() {
        try {
            String keep = Ui.id(lot);
            Ui.reload(lot, service.dao().allLots());
            Ui.reload(initiatedBy, service.dao().employees("QA"));
            if (keep != null) {
                select(keep);
            }
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
