package trace.ui;

import trace.TraceService;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

/** The main window: a header, one tab per user task, and a status bar showing the JDBC connection. */
public class MainFrame extends JFrame {
    final JTabbedPane tabs = new JTabbedPane();
    final LotsPanel lots;
    final ReceivePanel receive;
    final BatchPanel batch;
    final TestPanel test;
    final ShipmentPanel shipment;
    final TracePanel trace;

    public MainFrame(TraceService service, String connectionInfo) {
        super("Batch Recall Traceability");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel title = new JLabel("Batch Recall Traceability");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel sub = new JLabel("Food processing unit · lot genealogy, quality and recall");
        sub.setForeground(new Color(0xD5, 0xDA, 0xE0));
        header.add(title, BorderLayout.WEST);
        header.add(sub, BorderLayout.EAST);

        lots = new LotsPanel(service);
        receive = new ReceivePanel(service);
        batch = new BatchPanel(service);
        test = new TestPanel(service);
        shipment = new ShipmentPanel(service);
        trace = new TracePanel(service);
        tabs.addTab("Lots and stock", pad(lots));
        tabs.addTab("Receive raw lot", pad(receive));
        tabs.addTab("Production batch", pad(batch));
        tabs.addTab("Quality test", pad(test));
        tabs.addTab("Shipment", pad(shipment));
        tabs.addTab("Trace and recall", pad(trace));
        tabs.addChangeListener(e -> refreshSelected());

        JLabel status = new JLabel(connectionInfo);
        status.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        status.setForeground(new Color(0x2E, 0x6B, 0x3A));

        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        setSize(1500, 860);
        setLocationRelativeTo(null);
        refreshSelected();
    }

    private static JPanel pad(Component c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    void refreshSelected() {
        Component c = ((JPanel) tabs.getSelectedComponent()).getComponent(0);
        if (c instanceof Refreshable) {
            ((Refreshable) c).refresh();
        }
    }
}
