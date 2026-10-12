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

/** Screen 4 (QA officer): record a quality test. A FAIL puts the lot on HOLD. */
public class TestPanel extends JPanel implements Refreshable {
    private final TraceService service;
    final JComboBox<Option> lot = new JComboBox<Option>();
    final JTextField testDate = Ui.dateField(LocalDate.now());
    final JTextField parameter = Ui.field(16);
    final JComboBox<String> result = new JComboBox<String>(new String[]{"PASS", "FAIL"});
    final JComboBox<Option> testedBy = new JComboBox<Option>();
    final DefaultTableModel model = Ui.model("Lot", "Test no", "Date", "Parameter", "Result", "Tested by");

    public TestPanel(TraceService service) {
        super(new BorderLayout(10, 10));
        this.service = service;
        JPanel form = Ui.form("Record a quality test",
                "Lot", lot,
                "Test date (YYYY-MM-DD)", testDate,
                "Parameter tested", parameter,
                "Result", result,
                "Tested by", testedBy);
        JButton save = Ui.button("Save test");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(save);
        JPanel left = new JPanel(new BorderLayout());
        left.add(form, BorderLayout.NORTH);
        left.add(buttons, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);
        add(Ui.scroll(Ui.table(model), "Tests on the selected lot (weak entity QUALITY_TEST)"), BorderLayout.CENTER);
        save.addActionListener(e -> save());
        lot.addActionListener(e -> loadTests());
    }

    void save() {
        try {
            String msg = service.recordTest(Ui.id(lot), Ui.date(testDate, "Test date"), parameter.getText(),
                    (String) result.getSelectedItem(), Ui.id(testedBy));
            parameter.setText("");
            String keep = Ui.id(lot);
            refresh();
            select(keep);
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

    private void loadTests() {
        try {
            String id = Ui.id(lot);
            if (id != null) {
                Ui.fill(model, service.dao().qualityTests(id));
            }
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }

    @Override
    public void refresh() {
        try {
            Ui.reload(lot, service.dao().allLots());
            Ui.reload(testedBy, service.dao().employees("QA"));
            loadTests();
        } catch (Exception e) {
            Ui.error(this, e);
        }
    }
}
