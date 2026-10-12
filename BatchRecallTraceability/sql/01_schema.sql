-- =====================================================================
-- Batch Recall Traceability : DDL (Oracle 11g and later)
-- 15 tables from the Phase 1 relational schema, plus 2 views and 1 sequence.
-- Run order: 00_drop.sql (optional), 01_schema.sql, 02_sample_data.sql
-- =====================================================================

-- ---------- Master data ----------
CREATE TABLE supplier (
  supplier_id    VARCHAR2(10)  CONSTRAINT pk_supplier PRIMARY KEY,
  supplier_name  VARCHAR2(60)  NOT NULL,
  license_no     VARCHAR2(20)  NOT NULL CONSTRAINT uq_supplier_license UNIQUE,
  city           VARCHAR2(40)  NOT NULL
);

CREATE TABLE supplier_phone (
  supplier_id  VARCHAR2(10) NOT NULL
               CONSTRAINT fk_sphone_supplier REFERENCES supplier (supplier_id) ON DELETE CASCADE,
  phone        VARCHAR2(15) NOT NULL
               CONSTRAINT ck_sphone_len CHECK (LENGTH(phone) BETWEEN 8 AND 15),
  CONSTRAINT pk_supplier_phone PRIMARY KEY (supplier_id, phone)
);

CREATE TABLE item (
  item_id          VARCHAR2(10) CONSTRAINT pk_item PRIMARY KEY,
  item_name        VARCHAR2(60) NOT NULL,
  item_type        VARCHAR2(12) NOT NULL
                   CONSTRAINT ck_item_type CHECK (item_type IN ('RAW', 'INTERMEDIATE', 'FINISHED')),
  uom              VARCHAR2(5)  NOT NULL
                   CONSTRAINT ck_item_uom CHECK (uom IN ('KG', 'L', 'BOX')),
  shelf_life_days  NUMBER(5)    NOT NULL
                   CONSTRAINT ck_item_shelf CHECK (shelf_life_days > 0)
);

CREATE TABLE employee (
  emp_id    VARCHAR2(10) CONSTRAINT pk_employee PRIMARY KEY,
  emp_name  VARCHAR2(60) NOT NULL,
  role      VARCHAR2(12) NOT NULL
            CONSTRAINT ck_emp_role CHECK (role IN ('STORES', 'PRODUCTION', 'QA', 'DISPATCH', 'ADMIN'))
);

CREATE TABLE customer (
  customer_id    VARCHAR2(10) CONSTRAINT pk_customer PRIMARY KEY,
  customer_name  VARCHAR2(60) NOT NULL,
  customer_type  VARCHAR2(12) NOT NULL
                 CONSTRAINT ck_cust_type CHECK (customer_type IN ('DISTRIBUTOR', 'RETAILER', 'INSTITUTION')),
  phone          VARCHAR2(15) NOT NULL,
  city           VARCHAR2(40) NOT NULL
);

-- ---------- Production ----------
CREATE TABLE production_batch (
  batch_id       VARCHAR2(10) CONSTRAINT pk_batch PRIMARY KEY,
  batch_date     DATE         NOT NULL,
  line_no        NUMBER(2)    NOT NULL CONSTRAINT ck_batch_line CHECK (line_no BETWEEN 1 AND 10),
  shift          CHAR(1)      NOT NULL CONSTRAINT ck_batch_shift CHECK (shift IN ('A', 'B', 'C')),
  supervisor_id  VARCHAR2(10) NOT NULL CONSTRAINT fk_batch_supervisor REFERENCES employee (emp_id)
);

-- Superclass of the specialization: every lot, bought or made
CREATE TABLE lot (
  lot_id    VARCHAR2(10)  CONSTRAINT pk_lot PRIMARY KEY,
  item_id   VARCHAR2(10)  NOT NULL CONSTRAINT fk_lot_item REFERENCES item (item_id),
  quantity  NUMBER(10, 2) NOT NULL CONSTRAINT ck_lot_qty CHECK (quantity > 0),
  mfg_date  DATE          NOT NULL,
  status    VARCHAR2(10)  DEFAULT 'OK' NOT NULL
            CONSTRAINT ck_lot_status CHECK (status IN ('OK', 'HOLD', 'RECALLED'))
);

-- Subclass 1: lots bought from a supplier (shares the key of LOT)
CREATE TABLE raw_lot (
  lot_id         VARCHAR2(10) CONSTRAINT pk_raw_lot PRIMARY KEY
                 CONSTRAINT fk_rawlot_lot REFERENCES lot (lot_id) ON DELETE CASCADE,
  supplier_id    VARCHAR2(10) NOT NULL CONSTRAINT fk_rawlot_supplier REFERENCES supplier (supplier_id),
  received_date  DATE         NOT NULL,
  invoice_no     VARCHAR2(20) NOT NULL
);

-- Subclass 2: lots made by a production batch (shares the key of LOT)
CREATE TABLE produced_lot (
  lot_id    VARCHAR2(10) CONSTRAINT pk_produced_lot PRIMARY KEY
            CONSTRAINT fk_prodlot_lot REFERENCES lot (lot_id) ON DELETE CASCADE,
  batch_id  VARCHAR2(10) NOT NULL CONSTRAINT fk_prodlot_batch REFERENCES production_batch (batch_id)
);

-- M:N CONSUMES : which lots went into which batch
CREATE TABLE batch_input (
  batch_id  VARCHAR2(10)  NOT NULL CONSTRAINT fk_binput_batch REFERENCES production_batch (batch_id),
  lot_id    VARCHAR2(10)  NOT NULL CONSTRAINT fk_binput_lot REFERENCES lot (lot_id),
  qty_used  NUMBER(10, 2) NOT NULL CONSTRAINT ck_binput_qty CHECK (qty_used > 0),
  CONSTRAINT pk_batch_input PRIMARY KEY (batch_id, lot_id)
);

-- Weak entity: identified by its lot plus test_no
CREATE TABLE quality_test (
  lot_id     VARCHAR2(10) NOT NULL CONSTRAINT fk_test_lot REFERENCES lot (lot_id) ON DELETE CASCADE,
  test_no    NUMBER(3)    NOT NULL CONSTRAINT ck_test_no CHECK (test_no > 0),
  test_date  DATE         NOT NULL,
  parameter  VARCHAR2(40) NOT NULL,
  result     VARCHAR2(4)  NOT NULL CONSTRAINT ck_test_result CHECK (result IN ('PASS', 'FAIL')),
  tested_by  VARCHAR2(10) NOT NULL CONSTRAINT fk_test_emp REFERENCES employee (emp_id),
  CONSTRAINT pk_quality_test PRIMARY KEY (lot_id, test_no)
);

-- ---------- Distribution ----------
CREATE TABLE shipment (
  shipment_id  VARCHAR2(10) CONSTRAINT pk_shipment PRIMARY KEY,
  customer_id  VARCHAR2(10) NOT NULL CONSTRAINT fk_ship_customer REFERENCES customer (customer_id),
  ship_date    DATE         NOT NULL,
  vehicle_no   VARCHAR2(15) NOT NULL
);

-- M:N CONTAINS : which lots left on which shipment
CREATE TABLE shipment_line (
  shipment_id  VARCHAR2(10)  NOT NULL CONSTRAINT fk_sline_ship REFERENCES shipment (shipment_id) ON DELETE CASCADE,
  lot_id       VARCHAR2(10)  NOT NULL CONSTRAINT fk_sline_lot REFERENCES lot (lot_id),
  qty_shipped  NUMBER(10, 2) NOT NULL CONSTRAINT ck_sline_qty CHECK (qty_shipped > 0),
  CONSTRAINT pk_shipment_line PRIMARY KEY (shipment_id, lot_id)
);

-- ---------- Recall ----------
CREATE TABLE recall (
  recall_id       VARCHAR2(10)  CONSTRAINT pk_recall PRIMARY KEY,
  trigger_lot_id  VARCHAR2(10)  NOT NULL CONSTRAINT fk_recall_lot REFERENCES lot (lot_id),
  initiated_by    VARCHAR2(10)  NOT NULL CONSTRAINT fk_recall_emp REFERENCES employee (emp_id),
  recall_date     DATE          NOT NULL,
  reason          VARCHAR2(200) NOT NULL,
  severity        VARCHAR2(6)   NOT NULL CONSTRAINT ck_recall_sev CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),
  status          VARCHAR2(6)   DEFAULT 'OPEN' NOT NULL
                  CONSTRAINT ck_recall_status CHECK (status IN ('OPEN', 'CLOSED'))
);

-- M:N COVERS : lots found by the trace
CREATE TABLE recall_lot (
  recall_id     VARCHAR2(10) NOT NULL CONSTRAINT fk_rlot_recall REFERENCES recall (recall_id) ON DELETE CASCADE,
  lot_id        VARCHAR2(10) NOT NULL CONSTRAINT fk_rlot_lot REFERENCES lot (lot_id),
  direction     VARCHAR2(10) NOT NULL CONSTRAINT ck_rlot_dir CHECK (direction IN ('UPSTREAM', 'DOWNSTREAM')),
  action_taken  VARCHAR2(12) NOT NULL
                CONSTRAINT ck_rlot_action CHECK (action_taken IN ('BLOCKED', 'RETRIEVE', 'INVESTIGATE', 'DESTROYED')),
  CONSTRAINT pk_recall_lot PRIMARY KEY (recall_id, lot_id)
);

CREATE SEQUENCE recall_seq START WITH 2 INCREMENT BY 1 NOCACHE;

-- ---------- Views ----------
-- Derived attribute from the ER diagram: expiry_date is computed, never stored
CREATE VIEW lot_expiry AS
  SELECT l.lot_id, l.mfg_date, l.mfg_date + i.shelf_life_days AS expiry_date
  FROM lot l JOIN item i ON i.item_id = l.item_id;

-- Quantity still available in each lot = quantity - used in batches - shipped
CREATE VIEW lot_stock AS
  SELECT l.lot_id,
         l.quantity,
         NVL((SELECT SUM(bi.qty_used) FROM batch_input bi WHERE bi.lot_id = l.lot_id), 0)       AS qty_used,
         NVL((SELECT SUM(sl.qty_shipped) FROM shipment_line sl WHERE sl.lot_id = l.lot_id), 0) AS qty_shipped,
         l.quantity
           - NVL((SELECT SUM(bi.qty_used) FROM batch_input bi WHERE bi.lot_id = l.lot_id), 0)
           - NVL((SELECT SUM(sl.qty_shipped) FROM shipment_line sl WHERE sl.lot_id = l.lot_id), 0) AS qty_available
  FROM lot l;
