package trace.ui;

import trace.Model.LotQty;
import trace.Model.Option;
import trace.TraceService;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Screen 5 (dispatch clerk): record a shipment of finished lots to a customer. */
public class ShipmentPanel extends JPanel implements Refreshable {
    private final TraceService service;
    final JTextField shipmentId = Ui.field(10);
    final JComboBox<Option> customer = new JComboBox<Option>();
    final JTextField shipDate = Ui.dateField(LocalDate.now());
    final JTextField vehicle = Ui.field(12);
    final JComboBox<Option> lot = new JComboBox<Option>();
    final JTextField qty = Ui.field(6);
    final List<LotQty> lines = new ArrayList<LotQty>();
    final DefaultTableModel lineModel = Ui.model("Lot", "Quantity shipped");
    final JTable lineTable = Ui.table(lineModel);
    final DefaultTableModel model = Ui.model("Shipment", "Date", "Customer", "City", "Lot", "Quantity", "Vehicle");

    public ShipmentPanel(TraceService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        JPanel header = Ui.form("1. Shipment details",
                "Shipment number", shipmentId,
                "Customer", customer,
                "Ship date (YYYY-MM-DD)", shipDate,
                "Vehicle number", vehicle);
        JButton add = Ui.button("Add lot");
        JButton remove = new JButton("Remove selected");
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.add(qty);
        addRow.add(add);
        addRow.add(remove);
        JPanel lineForm = Ui.form("2. Finished lots loaded",
                "Lot", lot,
                "Quantity", addRow);
        JScrollPane lineScroll = new JScrollPane(lineTable);
        lineScroll.setPreferredSize(new Dimension(300, 110));
        JButton save = Ui.button("Save shipment");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(save);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(header);
        left.add(lineForm);
        left.add(lineScroll);
        left.add(buttons);
        JPanel west = new JPanel(new BorderLayout());
        west.add(left, BorderLayout.NORTH);
        west.setPreferredSize(new Dimension(640, 10));
        add(west, BorderLayout.WEST);
        add(Ui.scroll(Ui.widths(Ui.table(model), 80, 100, 260, 100, 60, 75, 110), "Shipments (SHIPMENT joined to SHIPMENT_LINE and CUSTOMER)"), BorderLayout.CENTER);
        add.addActionListener(e -> addLine());
        remove.addActionListener(e -> removeLine());
        save.addActionListener(e -> save());
    }

    void addLine() {
        try {
            String id = Ui.id(lot);
            double q = Ui.number(qty, "Quantity");
            lines.add(new LotQty(id, q));
            lineModel.addRow(new Object[]{id, qty.getText().trim()});
            qty.setText("");
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void removeLine() {
        int r = lineTable.getSelectedRow();
        if (r >= 0) {
            lines.remove(r);
            lineModel.removeRow(r);
        }
    }

    void save() {
        try {
            String msg = service.recordShipment(shipmentId.getText(), Ui.id(customer), Ui.date(shipDate, "Ship date"),
                    vehicle.getText(), lines);
            shipmentId.setText("");
            vehicle.setText("");
            lines.clear();
            lineModel.setRowCount(0);
            refresh();
            Ui.info(this, msg);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    @Override
    public void refresh() {
        try {
            Ui.reload(customer, service.dao().customers());
            Ui.reload(lot, service.dao().usableLots("FINISHED"));
            Ui.fill(model, service.dao().recentShipments());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
