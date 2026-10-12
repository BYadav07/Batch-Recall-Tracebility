package trace.ui;

import trace.BusinessRuleException;
import trace.Model.LotQty;
import trace.TraceDao;
import trace.TraceService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

/** Business-rule tests: loads the scripts into H2 (Oracle mode) and tests the rules. */
public class RuleTests {
    static int pass = 0, fail = 0;

    public static Connection freshDb(String name, String sqlDir) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:" + name + ";MODE=Oracle;DB_CLOSE_DELAY=-1", "TRACE_APP", "");
        for (String f : new String[]{"01_schema.sql", "02_sample_data.sql"}) {
            StringBuilder sb = new StringBuilder();
            for (String line : Files.readAllLines(Path.of(sqlDir, f))) {
                if (!line.strip().startsWith("--")) sb.append(line).append('\n');
            }
            for (String s : sb.toString().split(";")) {
                if (!s.isBlank()) try (Statement st = c.createStatement()) { st.execute(s); }
            }
        }
        c.setAutoCommit(false);
        return c;
    }

    interface Step { String run() throws Exception; }

    static void expectOk(String name, Step s) {
        try { String m = s.run(); System.out.println("PASS  " + name + "  ->  " + m); pass++; }
        catch (Exception e) { System.out.println("FAIL  " + name + "  !!  " + e); fail++; }
    }

    static void expectRule(String name, Step s) {
        try { String m = s.run(); System.out.println("FAIL  " + name + " (no error)  ->  " + m); fail++; }
        catch (BusinessRuleException e) { System.out.println("PASS  " + name + "  ->  rejected: " + e.getMessage()); pass++; }
        catch (Exception e) { System.out.println("FAIL  " + name + "  !!  " + e); fail++; }
    }

    public static void main(String[] a) throws Exception {
        Connection c = freshDb("logic", a[0]);
        TraceService s = new TraceService(new TraceDao(c));
        LocalDate today = LocalDate.now();

        System.out.println("== BR1 receive raw lot");
        expectOk("receive L-130 flour", () -> s.receiveRawLot("L-130", "I01", "S01", 600, today.minusDays(6), today.minusDays(1), "INV-4520"));
        expectRule("duplicate lot number", () -> s.receiveRawLot("L-130", "I01", "S01", 10, today.minusDays(6), today, "X"));
        expectRule("finished item cannot be received", () -> s.receiveRawLot("L-131", "I20", "S01", 10, today, today, "X"));
        expectRule("received in the future", () -> s.receiveRawLot("L-131", "I02", "S03", 10, today, today.plusDays(2), "X"));
        expectRule("zero quantity", () -> s.receiveRawLot("L-131", "I02", "S03", 0, today, today, "X"));

        System.out.println("== BR2 production batch");
        expectRule("expired dough L-201", () -> s.recordBatch("B-510", today, 1, "A", "E07",
                Arrays.asList(new LotQty("L-201", 20)), "L-310", "I21", 30));
        expectRule("recalled sugar L-095", () -> s.recordBatch("B-510", today, 1, "A", "E07",
                Arrays.asList(new LotQty("L-095", 20)), "L-310", "I10", 30));
        expectRule("more than available", () -> s.recordBatch("B-510", today, 1, "A", "E07",
                Arrays.asList(new LotQty("L-120", 500)), "L-206", "I11", 30));
        expectRule("no inputs", () -> s.recordBatch("B-510", today, 1, "A", "E07",
                Collections.<LotQty>emptyList(), "L-206", "I11", 30));
        expectRule("raw item as output", () -> s.recordBatch("B-510", today, 1, "A", "E07",
                Arrays.asList(new LotQty("L-101", 50)), "L-206", "I01", 30));
        expectOk("valid batch B-510 makes dough L-203", () -> s.recordBatch("B-510", today, 2, "B", "E08",
                Arrays.asList(new LotQty("L-130", 200), new LotQty("L-110", 30), new LotQty("L-114", 20)), "L-203", "I10", 240));
        expectOk("valid batch B-511 makes biscuits L-304", () -> s.recordBatch("B-511", today, 2, "B", "E08",
                Arrays.asList(new LotQty("L-203", 240)), "L-304", "I21", 450));

        System.out.println("== BR3 quality test");
        expectOk("PASS test on L-304", () -> s.recordTest("L-304", today, "Pack seal", "PASS", "E11"));
        expectOk("FAIL test puts L-302 on HOLD", () -> s.recordTest("L-302", today, "Moisture", "FAIL", "E11"));

        System.out.println("== BR4 shipment");
        expectRule("intermediate cannot ship", () -> s.recordShipment("S-93", "C02", today, "TN23ZZ0001",
                Arrays.asList(new LotQty("L-202", 1))));
        expectRule("HOLD lot cannot ship", () -> s.recordShipment("S-93", "C02", today, "TN23ZZ0001",
                Arrays.asList(new LotQty("L-302", 10))));
        expectRule("more than available", () -> s.recordShipment("S-93", "C02", today, "TN23ZZ0001",
                Arrays.asList(new LotQty("L-301", 150))));
        expectOk("valid shipment S-93", () -> s.recordShipment("S-93", "C02", today, "TN23ZZ0001",
                Arrays.asList(new LotQty("L-301", 60), new LotQty("L-304", 200))));

        System.out.println("== BR5 trace and recall");
        expectOk("trace L-205", () -> {
            TraceService.TraceResult r = s.trace("L-205");
            StringBuilder b = new StringBuilder("up=");
            r.upstream.forEach(t -> b.append(t.lotId).append('(').append(t.source).append(") "));
            b.append(" down=");
            r.downstream.forEach(t -> b.append(t.lotId).append(' '));
            b.append(" ships=");
            r.shipments.forEach(x -> b.append(x[0]).append('/').append(x[5]).append(' '));
            return b.toString();
        });
        expectOk("recall L-205", () -> s.raiseRecall("L-205", "E11", "Salmonella detected on retest", "HIGH"));
        expectRule("second open recall on L-205", () -> s.raiseRecall("L-205", "E11", "again", "HIGH"));
        expectOk("statuses after recall", () -> {
            StringBuilder b = new StringBuilder();
            for (String l : new String[]{"L-205", "L-301", "L-303", "L-120", "L-110", "L-114"})
                b.append(l).append('=').append(s.dao().lotInfo(l).status).append(' ');
            return b.toString();
        });

        System.out.println("== database constraint as last line of defence (bypassing the service)");
        expectOk("CHECK constraint rejects a negative quantity", () -> {
            try { s.dao().insertLot("L-999", "I01", -5, java.sql.Date.valueOf(today)); s.dao().commit(); return "NOT REJECTED"; }
            catch (SQLException e) { s.dao().rollback(); return "rejected by database: " + e.getMessage().split("\n")[0]; }
        });

        System.out.println("\n" + pass + " passed, " + fail + " failed");
    }
}
