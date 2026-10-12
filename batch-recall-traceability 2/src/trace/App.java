package trace;

import trace.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.sql.Connection;

/**
 * Entry point.
 *   java -cp "out;lib/ojdbc11.jar" trace.App            (Windows)
 *   java -cp "out:lib/ojdbc11.jar" trace.App            (Linux / macOS)
 * Reads db.properties from the working folder, or a path given as the first argument.
 */
public class App {
    public static void main(String[] args) {
        String config = args.length > 0 ? args[0] : "db.properties";
        setLookAndFeel();
        try {
            Db db = Db.fromFile(config);
            Connection con = db.connect();
            String info = Db.describe(con);
            System.out.println(info);
            TraceService service = new TraceService(new TraceDao(con));
            SwingUtilities.invokeLater(() -> new MainFrame(service, info).setVisible(true));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to the database.\n" + e.getMessage()
                            + "\n\nCheck db.properties and that the Oracle JDBC driver jar is on the classpath.",
                    "Connection failed", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.getLookAndFeelDefaults().put("defaultFont", new Font("SansSerif", Font.PLAIN, 14));
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
    }
}
