package trace.ui;

import trace.Model.LotQty;
import trace.Model.Option;
import trace.TraceService;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Screen 3 (production supervisor): record a batch, the lots it consumed and the lot it produced. */
public class BatchPanel extends JPanel implements Refreshable {
    private final TraceService service;
    final JTextField batchId = Ui.field(10);
    final JTextField batchDate = Ui.dateField(LocalDate.now());
    final JSpinner lineNo = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    final JComboBox<String> shift = new JComboBox<String>(new String[]{"A", "B", "C"});
    final JComboBox<Option> supervisor = new JComboBox<Option>();
    final JComboBox<Option> inputLot = new JComboBox<Option>();
    final JTextField inputQty = Ui.field(6);
    final List<LotQty> inputs = new ArrayList<LotQty>();
    final DefaultTableModel inputModel = Ui.model("Input lot", "Quantity used");
    final JTable inputTable = Ui.table(inputModel);
    final JTextField outLotId = Ui.field(10);
    final JComboBox<Option> outItem = new JComboBox<Option>();
    final JTextField outQty = Ui.field(8);
    final DefaultTableModel batchModel = Ui.model("Batch", "Date", "Line", "Shift", "Supervisor", "Inputs", "Output lot");

    public BatchPanel(TraceService service) {
        super(new BorderLayout(10, 10));
        this.service = service;

        JPanel header = Ui.form("1. Batch details",
                "Batch number", batchId,
                "Batch date (YYYY-MM-DD)", batchDate,
                "Line number", lineNo,
                "Shift", shift,
                "Supervisor", supervisor);

        JButton add = Ui.button("Add input");
        JButton remove = new JButton("Remove selected");
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        addRow.add(inputQty);
        addRow.add(add);
        addRow.add(remove);
        JPanel inputForm = Ui.form("2. Lots consumed (status OK, stock left)",
                "Lot", inputLot,
                "Quantity used", addRow);
        JPanel inputBox = new JPanel(new BorderLayout());
        inputBox.add(inputForm, BorderLayout.NORTH);
        javax.swing.JScrollPane inputScroll = new javax.swing.JScrollPane(inputTable);
        inputScroll.setPreferredSize(new Dimension(300, 110));
        inputBox.add(inputScroll, BorderLayout.CENTER);

        JPanel output = Ui.form("3. Lot produced",
                "Output lot number", outLotId,
                "Item (intermediate or finished)", outItem,
                "Quantity produced", outQty);

        JButton save = Ui.button("Save batch");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(save);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(header);
        left.add(inputBox);
        left.add(output);
        left.add(buttons);
        JPanel west = new JPanel(new BorderLayout());
        west.add(left, BorderLayout.NORTH);
        west.setPreferredSize(new Dimension(640, 10));
        add(west, BorderLayout.WEST);
        add(Ui.scroll(Ui.table(batchModel), "Production batches"), BorderLayout.CENTER);

        add.addActionListener(e -> addInput());
        remove.addActionListener(e -> removeInput());
        save.addActionListener(e -> save());
    }

    void addInput() {
        try {
            String lot = Ui.id(inputLot);
            double qty = Ui.number(inputQty, "Quantity used");
            inputs.add(new LotQty(lot, qty));
            inputModel.addRow(new Object[]{lot, inputQty.getText().trim()});
            inputQty.setText("");
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    private void removeInput() {
        int r = inputTable.getSelectedRow();
        if (r >= 0) {
            inputs.remove(r);
            inputModel.removeRow(r);
        }
    }

    void save() {
        try {
            String msg = service.recordBatch(batchId.getText(), Ui.date(batchDate, "Batch date"),
                    (Integer) lineNo.getValue(), (String) shift.getSelectedItem(), Ui.id(supervisor),
                    inputs, outLotId.getText(), Ui.id(outItem), Ui.number(outQty, "Quantity produced"));
            batchId.setText("");
            outLotId.setText("");
            outQty.setText("");
            inputs.clear();
            inputModel.setRowCount(0);
            refresh();
            Ui.info(this, msg);
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    @Override
    public void refresh() {
        try {
            Ui.reload(supervisor, service.dao().employees("PRODUCTION"));
            Ui.reload(inputLot, service.dao().usableLots("RAW", "INTERMEDIATE"));
            Ui.reload(outItem, service.dao().items("INTERMEDIATE", "FINISHED"));
            Ui.fill(batchModel, service.dao().recentBatches());
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
