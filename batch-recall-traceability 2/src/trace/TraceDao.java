package trace;

import trace.Model.LotInfo;
import trace.Model.Option;
import trace.Model.TraceRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data access: every SQL statement the application runs is in this class.
 * All statements use PreparedStatement with ? parameters (no string concatenation of user input).
 */
public class TraceDao {
    private final Connection con;

    public TraceDao(Connection con) {
        this.con = con;
    }

    public void commit() throws SQLException {
        con.commit();
    }

    public void rollback() {
        try {
            con.rollback();
        } catch (SQLException ignored) {
            // nothing more we can do; the original error is reported instead
        }
    }

    // ------------------------------------------------------------------ lookups (retrieval)

    public List<Option> suppliers() throws SQLException {
        return options("SELECT supplier_id, supplier_id || ' - ' || supplier_name FROM supplier ORDER BY supplier_id");
    }

    public List<Option> customers() throws SQLException {
        return options("SELECT customer_id, customer_id || ' - ' || customer_name || ', ' || city FROM customer ORDER BY customer_id");
    }

    public List<Option> employees(String role) throws SQLException {
        return options("SELECT emp_id, emp_id || ' - ' || emp_name FROM employee WHERE role = ? ORDER BY emp_id", role);
    }

    public List<Option> items(String... types) throws SQLException {
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            in.append(i == 0 ? "?" : ", ?");
        }
        return options("SELECT item_id, item_id || ' - ' || item_name || ' (' || uom || ')' FROM item"
                + " WHERE item_type IN (" + in + ") ORDER BY item_id", (Object[]) types);
    }

    /** Lots with stock left, status OK, of the given item types (for batch inputs and shipments). */
    public List<Option> usableLots(String... types) throws SQLException {
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            in.append(i == 0 ? "?" : ", ?");
        }
        List<Option> out = new ArrayList<Option>();
        for (Object[] r : rows("SELECT l.lot_id, i.item_name, s.qty_available, i.uom"
                + " FROM lot l JOIN item i ON i.item_id = l.item_id JOIN lot_stock s ON s.lot_id = l.lot_id"
                + " WHERE l.status = 'OK' AND s.qty_available > 0 AND i.item_type IN (" + in + ")"
                + " ORDER BY l.lot_id", (Object[]) types)) {
            out.add(new Option(String.valueOf(r[0]), r[0] + " - " + r[1] + " ("
                    + TraceService.fmt(((Number) r[2]).doubleValue()) + " " + r[3] + " left)"));
        }
        return out;
    }

    public List<Option> allLots() throws SQLException {
        return options("SELECT l.lot_id, l.lot_id || ' - ' || i.item_name || ' [' || l.status || ']'"
                + " FROM lot l JOIN item i ON i.item_id = l.item_id ORDER BY l.lot_id");
    }

    /** Every lot with its source, stock and expiry (the Lots screen). */
    public List<Object[]> lotOverview(String itemType) throws SQLException {
        String sql = "SELECT l.lot_id, i.item_name, i.item_type, l.status, l.quantity, s.qty_available, i.uom,"
                + " l.mfg_date, e.expiry_date,"
                + " CASE WHEN r.lot_id IS NOT NULL THEN 'Supplier ' || sp.supplier_name ELSE 'Batch ' || p.batch_id END"
                + " FROM lot l"
                + " JOIN item i ON i.item_id = l.item_id"
                + " JOIN lot_stock s ON s.lot_id = l.lot_id"
                + " JOIN lot_expiry e ON e.lot_id = l.lot_id"
                + " LEFT JOIN raw_lot r ON r.lot_id = l.lot_id"
                + " LEFT JOIN supplier sp ON sp.supplier_id = r.supplier_id"
                + " LEFT JOIN produced_lot p ON p.lot_id = l.lot_id"
                + (itemType == null ? "" : " WHERE i.item_type = ?")
                + " ORDER BY l.lot_id";
        return itemType == null ? rows(sql) : rows(sql, itemType);
    }

    public List<Object[]> qualityTests(String lotId) throws SQLException {
        return rows("SELECT q.lot_id, q.test_no, q.test_date, q.parameter, q.result, e.emp_name"
                + " FROM quality_test q JOIN employee e ON e.emp_id = q.tested_by"
                + " WHERE q.lot_id = ? ORDER BY q.test_no", lotId);
    }

    public List<Object[]> recentBatches() throws SQLException {
        return rows("SELECT b.batch_id, b.batch_date, b.line_no, b.shift, e.emp_name,"
                + " (SELECT COUNT(*) FROM batch_input bi WHERE bi.batch_id = b.batch_id),"
                + " (SELECT MIN(p.lot_id) FROM produced_lot p WHERE p.batch_id = b.batch_id)"
                + " FROM production_batch b JOIN employee e ON e.emp_id = b.supervisor_id"
                + " ORDER BY b.batch_date DESC, b.batch_id DESC");
    }

    public List<Object[]> recentShipments() throws SQLException {
        return rows("SELECT s.shipment_id, s.ship_date, c.customer_name, c.city, sl.lot_id, sl.qty_shipped, s.vehicle_no"
                + " FROM shipment s JOIN customer c ON c.customer_id = s.customer_id"
                + " JOIN shipment_line sl ON sl.shipment_id = s.shipment_id"
                + " ORDER BY s.ship_date DESC, s.shipment_id DESC, sl.lot_id");
    }

    public List<Object[]> rawLots() throws SQLException {
        return rows("SELECT r.lot_id, i.item_name, l.quantity, i.uom, sp.supplier_name, r.received_date, r.invoice_no, l.status"
                + " FROM raw_lot r JOIN lot l ON l.lot_id = r.lot_id JOIN item i ON i.item_id = l.item_id"
                + " JOIN supplier sp ON sp.supplier_id = r.supplier_id ORDER BY r.received_date DESC, r.lot_id DESC");
    }

    public LotInfo lotInfo(String lotId) throws SQLException {
        String sql = "SELECT l.lot_id, i.item_name, i.item_type, i.uom, l.status, l.quantity, s.qty_available,"
                + " s.qty_shipped, l.mfg_date, e.expiry_date"
                + " FROM lot l JOIN item i ON i.item_id = l.item_id"
                + " JOIN lot_stock s ON s.lot_id = l.lot_id JOIN lot_expiry e ON e.lot_id = l.lot_id"
                + " WHERE l.lot_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, lotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                LotInfo l = new LotInfo();
                l.lotId = rs.getString(1);
                l.itemName = rs.getString(2);
                l.itemType = rs.getString(3);
                l.uom = rs.getString(4);
                l.status = rs.getString(5);
                l.quantity = rs.getDouble(6);
                l.available = rs.getDouble(7);
                l.shipped = rs.getDouble(8);
                l.mfgDate = rs.getDate(9);
                l.expiryDate = rs.getDate(10);
                return l;
            }
        }
    }

    public String itemType(String itemId) throws SQLException {
        return single("SELECT item_type FROM item WHERE item_id = ?", itemId);
    }

    public boolean exists(String table, String keyColumn, String id) throws SQLException {
        // table and column names are constants from the service, never user input
        return single("SELECT COUNT(*) FROM " + table + " WHERE " + keyColumn + " = ?", id).equals("1");
    }

    public boolean openRecallExists(String lotId) throws SQLException {
        return !single("SELECT COUNT(*) FROM recall WHERE trigger_lot_id = ? AND status = 'OPEN'", lotId).equals("0");
    }

    // ------------------------------------------------------------------ inserts and updates (DML)

    public void insertLot(String lotId, String itemId, double qty, Date mfgDate) throws SQLException {
        update("INSERT INTO lot (lot_id, item_id, quantity, mfg_date, status) VALUES (?, ?, ?, ?, 'OK')",
                lotId, itemId, qty, mfgDate);
    }

    public void insertRawLot(String lotId, String supplierId, Date received, String invoiceNo) throws SQLException {
        update("INSERT INTO raw_lot (lot_id, supplier_id, received_date, invoice_no) VALUES (?, ?, ?, ?)",
                lotId, supplierId, received, invoiceNo);
    }

    public void insertBatch(String batchId, Date date, int line, String shift, String supervisorId) throws SQLException {
        update("INSERT INTO production_batch (batch_id, batch_date, line_no, shift, supervisor_id) VALUES (?, ?, ?, ?, ?)",
                batchId, date, line, shift, supervisorId);
    }

    public void insertBatchInput(String batchId, String lotId, double qty) throws SQLException {
        update("INSERT INTO batch_input (batch_id, lot_id, qty_used) VALUES (?, ?, ?)", batchId, lotId, qty);
    }

    public void insertProducedLot(String lotId, String batchId) throws SQLException {
        update("INSERT INTO produced_lot (lot_id, batch_id) VALUES (?, ?)", lotId, batchId);
    }

    public int nextTestNo(String lotId) throws SQLException {
        return Integer.parseInt(single("SELECT NVL(MAX(test_no), 0) + 1 FROM quality_test WHERE lot_id = ?", lotId));
    }

    public void insertTest(String lotId, int testNo, Date date, String parameter, String result, String empId)
            throws SQLException {
        update("INSERT INTO quality_test (lot_id, test_no, test_date, parameter, result, tested_by) VALUES (?, ?, ?, ?, ?, ?)",
                lotId, testNo, date, parameter, result, empId);
    }

    public void setLotStatus(String lotId, String status) throws SQLException {
        update("UPDATE lot SET status = ? WHERE lot_id = ?", status, lotId);
    }

    public void insertShipment(String shipmentId, String customerId, Date date, String vehicle) throws SQLException {
        update("INSERT INTO shipment (shipment_id, customer_id, ship_date, vehicle_no) VALUES (?, ?, ?, ?)",
                shipmentId, customerId, date, vehicle);
    }

    public void insertShipmentLine(String shipmentId, String lotId, double qty) throws SQLException {
        update("INSERT INTO shipment_line (shipment_id, lot_id, qty_shipped) VALUES (?, ?, ?)", shipmentId, lotId, qty);
    }

    public String nextRecallId() throws SQLException {
        return single("SELECT 'R-' || LPAD(recall_seq.NEXTVAL, 3, '0') FROM dual");
    }

    public void insertRecall(String recallId, String lotId, String empId, Date date, String reason, String severity)
            throws SQLException {
        update("INSERT INTO recall (recall_id, trigger_lot_id, initiated_by, recall_date, reason, severity, status)"
                + " VALUES (?, ?, ?, ?, ?, ?, 'OPEN')", recallId, lotId, empId, date, reason, severity);
    }

    public void insertRecallLot(String recallId, String lotId, String direction, String action) throws SQLException {
        update("INSERT INTO recall_lot (recall_id, lot_id, direction, action_taken) VALUES (?, ?, ?, ?)",
                recallId, lotId, direction, action);
    }

    // ------------------------------------------------------------------ the two recursive traces

    /** Every lot made from lotId, at any depth (forward / downstream). */
    public List<TraceRow> downstream(String lotId) throws SQLException {
        String sql = "WITH down (lot_id, lvl) AS ("
                + "  SELECT lot_id, 0 FROM lot WHERE lot_id = ?"
                + "  UNION ALL"
                + "  SELECT pl.lot_id, d.lvl + 1 FROM down d"
                + "  JOIN batch_input bi ON bi.lot_id = d.lot_id"
                + "  JOIN produced_lot pl ON pl.batch_id = bi.batch_id)"
                + " SELECT d.lot_id, MIN(d.lvl), i.item_name, l.status, MIN(p.batch_id)"
                + " FROM down d JOIN lot l ON l.lot_id = d.lot_id JOIN item i ON i.item_id = l.item_id"
                + " JOIN produced_lot p ON p.lot_id = d.lot_id"
                + " WHERE d.lvl > 0"
                + " GROUP BY d.lot_id, i.item_name, l.status ORDER BY 2, 1";
        return traceRows(sql, lotId, "made in batch ");
    }

    /** Every input lot behind lotId, at any depth (backward / upstream), with the supplier of raw lots. */
    public List<TraceRow> upstream(String lotId) throws SQLException {
        String sql = "WITH up (lot_id, lvl) AS ("
                + "  SELECT lot_id, 0 FROM lot WHERE lot_id = ?"
                + "  UNION ALL"
                + "  SELECT bi.lot_id, u.lvl + 1 FROM up u"
                + "  JOIN produced_lot pl ON pl.lot_id = u.lot_id"
                + "  JOIN batch_input bi ON bi.batch_id = pl.batch_id)"
                + " SELECT u.lot_id, MIN(u.lvl), i.item_name, l.status,"
                + "  MIN(CASE WHEN sp.supplier_name IS NOT NULL THEN 'supplier ' || sp.supplier_name || ', ' || r.invoice_no"
                + "      ELSE 'made in batch ' || p.batch_id END)"
                + " FROM up u JOIN lot l ON l.lot_id = u.lot_id JOIN item i ON i.item_id = l.item_id"
                + " LEFT JOIN raw_lot r ON r.lot_id = u.lot_id LEFT JOIN supplier sp ON sp.supplier_id = r.supplier_id"
                + " LEFT JOIN produced_lot p ON p.lot_id = u.lot_id"
                + " WHERE u.lvl > 0"
                + " GROUP BY u.lot_id, i.item_name, l.status ORDER BY 2, 1";
        return traceRows(sql, lotId, "");
    }

    /** Shipments that carried any of the given lots, with the customer to contact. */
    public List<Object[]> shipmentsFor(Collection<String> lotIds) throws SQLException {
        if (lotIds.isEmpty()) {
            return new ArrayList<Object[]>();
        }
        StringBuilder in = new StringBuilder();
        Object[] params = lotIds.toArray();
        for (int i = 0; i < params.length; i++) {
            in.append(i == 0 ? "?" : ", ?");
        }
        return rows("SELECT s.shipment_id, s.ship_date, c.customer_name, c.city, c.phone, sl.lot_id, sl.qty_shipped"
                + " FROM shipment_line sl JOIN shipment s ON s.shipment_id = sl.shipment_id"
                + " JOIN customer c ON c.customer_id = s.customer_id"
                + " WHERE sl.lot_id IN (" + in + ") ORDER BY s.ship_date, s.shipment_id, sl.lot_id", params);
    }

    public List<Object[]> recalls() throws SQLException {
        return rows("SELECT r.recall_id, r.trigger_lot_id, r.recall_date, r.severity, r.status, r.reason,"
                + " (SELECT COUNT(*) FROM recall_lot rl WHERE rl.recall_id = r.recall_id)"
                + " FROM recall r ORDER BY r.recall_id DESC");
    }

    // ------------------------------------------------------------------ helpers

    private List<TraceRow> traceRows(String sql, String lotId, String sourcePrefix) throws SQLException {
        List<TraceRow> out = new ArrayList<TraceRow>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, lotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TraceRow t = new TraceRow();
                    t.lotId = rs.getString(1);
                    t.steps = rs.getInt(2);
                    t.itemName = rs.getString(3);
                    t.status = rs.getString(4);
                    t.source = sourcePrefix + rs.getString(5);
                    out.add(t);
                }
            }
        }
        return out;
    }

    private List<Option> options(String sql, Object... params) throws SQLException {
        List<Option> out = new ArrayList<Option>();
        for (Object[] r : rows(sql, params)) {
            out.add(new Option(String.valueOf(r[0]), String.valueOf(r[1])));
        }
        return out;
    }

    private List<Object[]> rows(String sql, Object... params) throws SQLException {
        List<Object[]> out = new ArrayList<Object[]>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] r = new Object[n];
                    for (int i = 0; i < n; i++) {
                        Object v = rs.getObject(i + 1);
                        if (v instanceof java.util.Date) {
                            v = rs.getDate(i + 1);
                        }
                        r[i] = v;
                    }
                    out.add(r);
                }
            }
        }
        return out;
    }

    private String single(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private int update(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof Date) {
                ps.setDate(i + 1, (Date) p);
            } else if (p instanceof Double) {
                ps.setDouble(i + 1, (Double) p);
            } else if (p instanceof Integer) {
                ps.setInt(i + 1, (Integer) p);
            } else {
                ps.setString(i + 1, p == null ? null : p.toString());
            }
        }
    }
}
