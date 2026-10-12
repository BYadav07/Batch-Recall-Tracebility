package trace;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens the JDBC connection. Settings come from db.properties:
 *   db.url      = jdbc:oracle:thin:@localhost:1521/XEPDB1
 *   db.user     = trace_app
 *   db.password = trace_app
 */
public final class Db {
    private final String url;
    private final String user;
    private final String password;

    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Db fromFile(String path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(path)) {
            p.load(in);
        }
        return new Db(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password"));
    }

    /** Opens a connection with auto-commit off, so every operation is one transaction. */
    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        return c;
    }

    /** One-line description of the connection, shown in the status bar. */
    public static String describe(Connection c) throws SQLException {
        DatabaseMetaData m = c.getMetaData();
        String version = m.getDatabaseProductVersion().split("\n")[0];
        return "Connected via JDBC to " + m.getDatabaseProductName() + " " + version
                + " as " + m.getUserName() + "  |  driver " + m.getDriverName() + " " + m.getDriverVersion();
    }
}
