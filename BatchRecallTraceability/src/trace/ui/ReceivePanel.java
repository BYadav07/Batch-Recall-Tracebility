package trace.ui;

import trace.Model.Option;
import trace.TraceService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;

/** Screen 2 (stores keeper): register a raw-material lot received from a supplier. */
public class ReceivePanel extends JPanel implements Refreshable {
    private final TraceService service;
    final JTextField lotId = Ui.field(10);
    final JComboBox<Option> item = new JComboBox<Option>();
    final JComboBox<Option> supplier = new JComboBox<Option>();
    final JTextField quantity = Ui.field(8);
    final JTextField mfgDate = Ui.dateField(LocalDate.now().minusDays(5));
    final JTextField receivedDate = Ui.dateField(LocalDate.now());
    final JTextField invoice = Ui.field(12);
    final DefaultTableModel model = Ui.model("Lot", "Item", "Quantity", "Unit", "Supplier", "Received", "Invoice", "Status");

    public ReceivePanel(TraceService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        JPanel form = Ui.form("Receive a raw-material lot",
                "Lot number", lotId,
                "Item (raw materials only)", item,
                "Supplier", supplier,
                "Quantity", quantity,
                "Manufactured on (YYYY-MM-DD)", mfgDate,
                "Received on (YYYY-MM-DD)", receivedDate,
                "Supplier invoice number", invoice);
        JButton save = Ui.button("Save lot");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(save);
        JPanel left = new JPanel(new BorderLayout());
        left.add(form, BorderLayout.NORTH);
        left.add(buttons, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);
        add(Ui.scroll(Ui.widths(Ui.table(model), 60, 110, 75, 45, 120, 100, 90, 80), "Raw lots received (RAW_LOT joined to LOT, ITEM, SUPPLIER)"), BorderLayout.CENTER);
        save.addActionListener(e -> save());
    }

    private void save() {
        try {
            String msg = service.receiveRawLot(lotId.getText(), Ui.id(item), Ui.id(supplier),
                    Ui.number(quantity, "Quantity"), Ui.date(mfgDate, "Manufactured on"),
                    Ui.date(receivedDate, "Received on"), invoice.getText());
            lotId.setText("");
            quantity.setText("");
            invoice.setText("");
            refresh();
            Ui.info(this, msg);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    @Override
    public void refresh() {
        try {
            Ui.reload(item, service.dao().items("RAW"));
            Ui.reload(supplier, service.dao().suppliers());
            Ui.fill(model, service.dao().rawLots());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
