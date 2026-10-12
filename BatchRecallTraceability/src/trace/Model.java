package trace;

import java.sql.Date;

/** Small value classes passed between the screens, the service and the DAO. */
public final class Model {
    private Model() {
    }

    /** An id plus a readable label, used in drop-down lists. */
    public static final class Option {
        public final String id;
        public final String label;

        public Option(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** What the business rules need to know about one lot. */
    public static final class LotInfo {
        public String lotId;
        public String itemName;
        public String itemType;
        public String uom;
        public String status;
        public double quantity;
        public double available;
        public double shipped;
        public Date mfgDate;
        public Date expiryDate;
    }

    /** One input line of a production batch, or one line of a shipment. */
    public static final class LotQty {
        public final String lotId;
        public final double qty;

        public LotQty(String lotId, double qty) {
            this.lotId = lotId;
            this.qty = qty;
        }
    }

    /** One lot found by a trace. */
    public static final class TraceRow {
        public String lotId;
        public int steps;
        public String itemName;
        public String status;
        public String source;
    }
}
