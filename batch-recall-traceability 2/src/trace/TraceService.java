package trace;

import trace.Model.LotInfo;
import trace.Model.LotQty;
import trace.Model.TraceRow;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic behind the screens. Each public method checks the business rules,
 * then writes through the DAO inside one transaction: all rows are saved, or none are.
 */
public class TraceService {
    private final TraceDao dao;

    public TraceService(TraceDao dao) {
        this.dao = dao;
    }

    public TraceDao dao() {
        return dao;
    }

    // ------------------------------------------------------------------ BR1 receive a raw lot

    public String receiveRawLot(String lotId, String itemId, String supplierId, double qty,
                                LocalDate mfgDate, LocalDate receivedDate, String invoiceNo)
            throws BusinessRuleException, SQLException {
        lotId = requiredId(lotId, "Lot number");
        invoiceNo = requiredId(invoiceNo, "Invoice number");
        required(itemId, "Item");
        required(supplierId, "Supplier");
        if (dao.exists("lot", "lot_id", lotId)) {
            throw new BusinessRuleException("Lot " + lotId + " already exists. Every lot number must be unique.");
        }
        if (!"RAW".equals(dao.itemType(itemId))) {
            throw new BusinessRuleException("Only raw materials can be received from a supplier.");
        }
        if (qty <= 0) {
            throw new BusinessRuleException("Quantity must be more than zero.");
        }
        if (receivedDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Received date cannot be in the future.");
        }
        if (mfgDate.isAfter(receivedDate)) {
            throw new BusinessRuleException("Manufacturing date cannot be after the received date.");
        }
        try {
            dao.insertLot(lotId, itemId, qty, Date.valueOf(mfgDate));
            dao.insertRawLot(lotId, supplierId, Date.valueOf(receivedDate), invoiceNo);
            dao.commit();
        } catch (SQLException e) {
            dao.rollback();
            throw e;
        }
        return "Raw lot " + lotId + " received and saved.";
    }

    // ------------------------------------------------------------------ BR2 record a production batch

    public String recordBatch(String batchId, LocalDate batchDate, int lineNo, String shift, String supervisorId,
                              List<LotQty> inputs, String outLotId, String outItemId, double outQty)
            throws BusinessRuleException, SQLException {
        batchId = requiredId(batchId, "Batch number");
        outLotId = requiredId(outLotId, "Output lot number");
        required(supervisorId, "Supervisor");
        required(outItemId, "Output item");
        if (dao.exists("production_batch", "batch_id", batchId)) {
            throw new BusinessRuleException("Batch " + batchId + " already exists.");
        }
        if (batchDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Batch date cannot be in the future.");
        }
        if (inputs.isEmpty()) {
            throw new BusinessRuleException("Add at least one input lot. A batch must consume something.");
        }
        Set<String> seen = new HashSet<String>();
        for (LotQty in : inputs) {
            if (!seen.add(in.lotId)) {
                throw new BusinessRuleException("Lot " + in.lotId + " is listed twice. Combine it into one line.");
            }
            LotInfo lot = dao.lotInfo(in.lotId);
            if (lot == null) {
                throw new BusinessRuleException("Input lot " + in.lotId + " does not exist.");
            }
            if (!"OK".equals(lot.status)) {
                throw new BusinessRuleException("Lot " + in.lotId + " is " + lot.status + " and cannot be used in production.");
            }
            if ("FINISHED".equals(lot.itemType)) {
                throw new BusinessRuleException("Lot " + in.lotId + " is a finished product and cannot be an input.");
            }
            if (lot.mfgDate.toLocalDate().isAfter(batchDate)) {
                throw new BusinessRuleException("Lot " + in.lotId + " was made after the batch date.");
            }
            if (lot.expiryDate.toLocalDate().isBefore(batchDate)) {
                throw new BusinessRuleException("Lot " + in.lotId + " (" + lot.itemName + ") expired on "
                        + lot.expiryDate + " and cannot be used on " + batchDate + ".");
            }
            if (in.qty <= 0) {
                throw new BusinessRuleException("Quantity used from lot " + in.lotId + " must be more than zero.");
            }
            if (in.qty > lot.available) {
                throw new BusinessRuleException("Lot " + in.lotId + " has only " + fmt(lot.available) + " " + lot.uom
                        + " left; " + fmt(in.qty) + " was entered.");
            }
        }
        if (dao.exists("lot", "lot_id", outLotId)) {
            throw new BusinessRuleException("Output lot " + outLotId + " already exists.");
        }
        if ("RAW".equals(dao.itemType(outItemId))) {
            throw new BusinessRuleException("A batch must produce an intermediate or finished item, not a raw material.");
        }
        if (outQty <= 0) {
            throw new BusinessRuleException("Output quantity must be more than zero.");
        }
        try {
            dao.insertBatch(batchId, Date.valueOf(batchDate), lineNo, shift, supervisorId);
            for (LotQty in : inputs) {
                dao.insertBatchInput(batchId, in.lotId, in.qty);
            }
            dao.insertLot(outLotId, outItemId, outQty, Date.valueOf(batchDate));
            dao.insertProducedLot(outLotId, batchId);
            dao.commit();
        } catch (SQLException e) {
            dao.rollback();
            throw e;
        }
        return "Batch " + batchId + " saved: " + inputs.size() + " input lot(s) consumed, output lot " + outLotId + " created.";
    }

    // ------------------------------------------------------------------ BR3 record a quality test

    public String recordTest(String lotId, LocalDate testDate, String parameter, String result, String testedBy)
            throws BusinessRuleException, SQLException {
        required(lotId, "Lot");
        parameter = required(parameter, "Parameter tested");
        required(testedBy, "Tested by");
        LotInfo lot = dao.lotInfo(lotId);
        if (lot == null) {
            throw new BusinessRuleException("Lot " + lotId + " does not exist.");
        }
        if (testDate.isAfter(LocalDate.now()) || testDate.isBefore(lot.mfgDate.toLocalDate())) {
            throw new BusinessRuleException("Test date must be between the lot's manufacturing date and today.");
        }
        String message;
        try {
            int testNo = dao.nextTestNo(lotId);
            dao.insertTest(lotId, testNo, Date.valueOf(testDate), parameter, result, testedBy);
            message = "Test " + testNo + " on lot " + lotId + " saved: " + result + ".";
            if ("FAIL".equals(result) && "OK".equals(lot.status)) {
                dao.setLotStatus(lotId, "HOLD");
                message += " The lot is now on HOLD and cannot be used or shipped until QA releases it.";
            }
            dao.commit();
        } catch (SQLException e) {
            dao.rollback();
            throw e;
        }
        return message;
    }

    // ------------------------------------------------------------------ BR4 record a shipment

    public String recordShipment(String shipmentId, String customerId, LocalDate shipDate, String vehicleNo,
                                 List<LotQty> lines) throws BusinessRuleException, SQLException {
        shipmentId = requiredId(shipmentId, "Shipment number");
        vehicleNo = requiredId(vehicleNo, "Vehicle number");
        required(customerId, "Customer");
        if (dao.exists("shipment", "shipment_id", shipmentId)) {
            throw new BusinessRuleException("Shipment " + shipmentId + " already exists.");
        }
        if (shipDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Ship date cannot be in the future.");
        }
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Add at least one lot to the shipment.");
        }
        Set<String> seen = new HashSet<String>();
        for (LotQty line : lines) {
            if (!seen.add(line.lotId)) {
                throw new BusinessRuleException("Lot " + line.lotId + " is listed twice.");
            }
            LotInfo lot = dao.lotInfo(line.lotId);
            if (lot == null) {
                throw new BusinessRuleException("Lot " + line.lotId + " does not exist.");
            }
            if (!"FINISHED".equals(lot.itemType)) {
                throw new BusinessRuleException("Only finished goods can be shipped; " + line.lotId + " is " + lot.itemType + ".");
            }
            if (!"OK".equals(lot.status)) {
                throw new BusinessRuleException("Lot " + line.lotId + " is " + lot.status + " and is blocked from shipment.");
            }
            if (shipDate.isBefore(lot.mfgDate.toLocalDate())) {
                throw new BusinessRuleException("Lot " + line.lotId + " cannot ship before it was made.");
            }
            if (lot.expiryDate.toLocalDate().isBefore(shipDate)) {
                throw new BusinessRuleException("Lot " + line.lotId + " expired on " + lot.expiryDate + ".");
            }
            if (line.qty <= 0 || line.qty > lot.available) {
                throw new BusinessRuleException("Lot " + line.lotId + " has " + fmt(lot.available) + " " + lot.uom
                        + " available; " + fmt(line.qty) + " was entered.");
            }
        }
        try {
            dao.insertShipment(shipmentId, customerId, Date.valueOf(shipDate), vehicleNo);
            for (LotQty line : lines) {
                dao.insertShipmentLine(shipmentId, line.lotId, line.qty);
            }
            dao.commit();
        } catch (SQLException e) {
            dao.rollback();
            throw e;
        }
        return "Shipment " + shipmentId + " saved with " + lines.size() + " lot(s).";
    }

    // ------------------------------------------------------------------ BR5 trace and recall

    public static final class TraceResult {
        public LotInfo lot;
        public List<TraceRow> upstream;
        public List<TraceRow> downstream;
        public List<Object[]> shipments;
    }

    public TraceResult trace(String lotId) throws BusinessRuleException, SQLException {
        required(lotId, "Lot");
        TraceResult r = new TraceResult();
        r.lot = dao.lotInfo(lotId);
        if (r.lot == null) {
            throw new BusinessRuleException("Lot " + lotId + " does not exist.");
        }
        r.upstream = dao.upstream(lotId);
        r.downstream = dao.downstream(lotId);
        Set<String> affected = new LinkedHashSet<String>();
        affected.add(lotId);
        for (TraceRow t : r.downstream) {
            affected.add(t.lotId);
        }
        r.shipments = dao.shipmentsFor(affected);
        return r;
    }

    /**
     * Raises a recall in one transaction:
     * the defective lot and everything made from it become RECALLED (DOWNSTREAM),
     * the lots it was made from go on HOLD for investigation (UPSTREAM).
     */
    public String raiseRecall(String lotId, String initiatedBy, String reason, String severity)
            throws BusinessRuleException, SQLException {
        reason = required(reason, "Reason");
        required(initiatedBy, "Initiated by");
        TraceResult t = trace(lotId);
        if (dao.openRecallExists(lotId)) {
            throw new BusinessRuleException("An open recall already exists for lot " + lotId + ".");
        }
        String recallId;
        int recalled = 1;
        int held = 0;
        try {
            recallId = dao.nextRecallId();
            dao.insertRecall(recallId, lotId, initiatedBy, Date.valueOf(LocalDate.now()), reason, severity);
            dao.setLotStatus(lotId, "RECALLED");
            for (TraceRow d : t.downstream) {
                LotInfo l = dao.lotInfo(d.lotId);
                dao.insertRecallLot(recallId, d.lotId, "DOWNSTREAM", l.shipped > 0 ? "RETRIEVE" : "BLOCKED");
                dao.setLotStatus(d.lotId, "RECALLED");
                recalled++;
            }
            for (TraceRow u : t.upstream) {
                dao.insertRecallLot(recallId, u.lotId, "UPSTREAM", "INVESTIGATE");
                if ("OK".equals(u.status)) {
                    dao.setLotStatus(u.lotId, "HOLD");
                    held++;
                }
            }
            dao.commit();
        } catch (SQLException e) {
            dao.rollback();
            throw e;
        }
        List<String> customers = new ArrayList<String>();
        for (Object[] s : t.shipments) {
            String c = String.valueOf(s[2]);
            if (!customers.contains(c)) {
                customers.add(c);
            }
        }
        return "Recall " + recallId + " raised. " + recalled + " lot(s) marked RECALLED, " + held
                + " upstream lot(s) put on HOLD, " + t.shipments.size() + " shipment line(s) to retrieve from "
                + customers.size() + " customer(s).";
    }

    // ------------------------------------------------------------------ helpers

    private static String required(String value, String field) throws BusinessRuleException {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessRuleException(field + " is required.");
        }
        return value.trim();
    }

    private static String requiredId(String value, String field) throws BusinessRuleException {
        return required(value, field).toUpperCase();
    }

    static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
