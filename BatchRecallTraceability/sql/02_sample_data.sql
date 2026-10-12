-- =====================================================================
-- Batch Recall Traceability : sample data (DML)
-- A small biscuit plant. All names and numbers are illustrative.
-- Genealogy is three levels deep:
--   raw lot -> intermediate lot (dough / cream filling) -> finished lot -> shipment
-- =====================================================================

-- ---------- Suppliers ----------
INSERT INTO supplier VALUES ('S01', 'Sree Mills',     'FSL-10012345', 'Coimbatore');
INSERT INTO supplier VALUES ('S02', 'Kaveri Oils',    'FSL-10023456', 'Mysuru');
INSERT INTO supplier VALUES ('S03', 'Arun Traders',   'FSL-10034567', 'Erode');
INSERT INTO supplier VALUES ('S04', 'Sree Dairy',     'FSL-10045678', 'Salem');

INSERT INTO supplier_phone VALUES ('S01', '0422-2456781');
INSERT INTO supplier_phone VALUES ('S01', '9843012345');
INSERT INTO supplier_phone VALUES ('S02', '0821-2345678');
INSERT INTO supplier_phone VALUES ('S03', '9894056789');
INSERT INTO supplier_phone VALUES ('S04', '0427-2412345');
INSERT INTO supplier_phone VALUES ('S04', '9790067890');

-- ---------- Items ----------
INSERT INTO item VALUES ('I01', 'Wheat flour',           'RAW',          'KG',  180);
INSERT INTO item VALUES ('I02', 'Sugar',                 'RAW',          'KG',  365);
INSERT INTO item VALUES ('I03', 'Palm oil',              'RAW',          'L',   270);
INSERT INTO item VALUES ('I04', 'Milk powder',           'RAW',          'KG',  300);
INSERT INTO item VALUES ('I10', 'Biscuit dough',         'INTERMEDIATE', 'KG',  2);
INSERT INTO item VALUES ('I11', 'Cream filling',         'INTERMEDIATE', 'KG',  10);
INSERT INTO item VALUES ('I20', 'Cream biscuits 200 g',  'FINISHED',     'BOX', 180);
INSERT INTO item VALUES ('I21', 'Plain biscuits 200 g',  'FINISHED',     'BOX', 180);

-- ---------- Employees ----------
INSERT INTO employee VALUES ('E01', 'Ravi Kumar',   'STORES');
INSERT INTO employee VALUES ('E07', 'Meena Iyer',   'PRODUCTION');
INSERT INTO employee VALUES ('E08', 'Arjun Das',    'PRODUCTION');
INSERT INTO employee VALUES ('E11', 'Priya Nair',   'QA');
INSERT INTO employee VALUES ('E15', 'Suresh Babu',  'DISPATCH');
INSERT INTO employee VALUES ('E20', 'Anita Rao',    'ADMIN');

-- ---------- Customers ----------
INSERT INTO customer VALUES ('C01', 'Chennai Foods Distributors', 'DISTRIBUTOR', '044-24567890', 'Chennai');
INSERT INTO customer VALUES ('C02', 'Vellore Supermart',          'RETAILER',    '0416-2234567', 'Vellore');
INSERT INTO customer VALUES ('C03', 'Bengaluru FMCG Distributors','DISTRIBUTOR', '080-41234567', 'Bengaluru');
INSERT INTO customer VALUES ('C04', 'Campus Hostel Mess',         'INSTITUTION', '0416-2243091', 'Vellore');

-- ---------- Raw lots (LOT row first, then RAW_LOT row) ----------
INSERT INTO lot VALUES ('L-091', 'I01', 500,  DATE '2026-08-20', 'OK');
INSERT INTO raw_lot VALUES ('L-091', 'S01', DATE '2026-08-26', 'INV-4380');
INSERT INTO lot VALUES ('L-095', 'I02', 200,  DATE '2026-08-15', 'RECALLED');
INSERT INTO raw_lot VALUES ('L-095', 'S03', DATE '2026-08-28', 'INV-0745');
INSERT INTO lot VALUES ('L-101', 'I01', 1000, DATE '2026-09-02', 'OK');
INSERT INTO raw_lot VALUES ('L-101', 'S01', DATE '2026-09-08', 'INV-4410');
INSERT INTO lot VALUES ('L-102', 'I01', 800,  DATE '2026-09-10', 'OK');
INSERT INTO raw_lot VALUES ('L-102', 'S01', DATE '2026-09-15', 'INV-4466');
INSERT INTO lot VALUES ('L-110', 'I02', 500,  DATE '2026-08-30', 'OK');
INSERT INTO raw_lot VALUES ('L-110', 'S03', DATE '2026-09-08', 'INV-0771');
INSERT INTO lot VALUES ('L-114', 'I03', 300,  DATE '2026-09-01', 'OK');
INSERT INTO raw_lot VALUES ('L-114', 'S02', DATE '2026-09-09', 'INV-2208');
INSERT INTO lot VALUES ('L-120', 'I04', 200,  DATE '2026-09-04', 'OK');
INSERT INTO raw_lot VALUES ('L-120', 'S04', DATE '2026-09-10', 'INV-4471');

-- ---------- August: one batch chain that was later recalled ----------
INSERT INTO production_batch VALUES ('B-490', DATE '2026-08-29', 1, 'A', 'E07');
INSERT INTO batch_input VALUES ('B-490', 'L-091', 200);
INSERT INTO batch_input VALUES ('B-490', 'L-095', 30);
INSERT INTO lot VALUES ('L-190', 'I10', 225, DATE '2026-08-29', 'RECALLED');
INSERT INTO produced_lot VALUES ('L-190', 'B-490');

INSERT INTO production_batch VALUES ('B-491', DATE '2026-08-29', 1, 'B', 'E07');
INSERT INTO batch_input VALUES ('B-491', 'L-190', 225);
INSERT INTO lot VALUES ('L-290', 'I21', 420, DATE '2026-08-29', 'RECALLED');
INSERT INTO produced_lot VALUES ('L-290', 'B-491');

-- ---------- September: intermediates ----------
INSERT INTO production_batch VALUES ('B-501', DATE '2026-09-12', 1, 'A', 'E07');
INSERT INTO batch_input VALUES ('B-501', 'L-101', 250);
INSERT INTO batch_input VALUES ('B-501', 'L-110', 60);
INSERT INTO batch_input VALUES ('B-501', 'L-114', 40);
INSERT INTO lot VALUES ('L-201', 'I10', 340, DATE '2026-09-12', 'OK');
INSERT INTO produced_lot VALUES ('L-201', 'B-501');

INSERT INTO production_batch VALUES ('B-502', DATE '2026-09-12', 2, 'A', 'E08');
INSERT INTO batch_input VALUES ('B-502', 'L-120', 50);
INSERT INTO batch_input VALUES ('B-502', 'L-110', 40);
INSERT INTO batch_input VALUES ('B-502', 'L-114', 30);
INSERT INTO lot VALUES ('L-205', 'I11', 115, DATE '2026-09-12', 'OK');
INSERT INTO produced_lot VALUES ('L-205', 'B-502');

INSERT INTO production_batch VALUES ('B-504', DATE '2026-09-16', 3, 'C', 'E08');
INSERT INTO batch_input VALUES ('B-504', 'L-102', 300);
INSERT INTO batch_input VALUES ('B-504', 'L-110', 50);
INSERT INTO batch_input VALUES ('B-504', 'L-114', 35);
INSERT INTO lot VALUES ('L-202', 'I10', 380, DATE '2026-09-16', 'OK');
INSERT INTO produced_lot VALUES ('L-202', 'B-504');

-- ---------- September: finished goods ----------
INSERT INTO production_batch VALUES ('B-503', DATE '2026-09-13', 1, 'B', 'E07');
INSERT INTO batch_input VALUES ('B-503', 'L-201', 300);
INSERT INTO batch_input VALUES ('B-503', 'L-205', 100);
INSERT INTO lot VALUES ('L-301', 'I20', 600, DATE '2026-09-13', 'OK');
INSERT INTO produced_lot VALUES ('L-301', 'B-503');

INSERT INTO production_batch VALUES ('B-505', DATE '2026-09-17', 3, 'A', 'E08');
INSERT INTO batch_input VALUES ('B-505', 'L-202', 370);
INSERT INTO lot VALUES ('L-302', 'I21', 700, DATE '2026-09-17', 'OK');
INSERT INTO produced_lot VALUES ('L-302', 'B-505');

INSERT INTO production_batch VALUES ('B-506', DATE '2026-09-17', 1, 'A', 'E07');
INSERT INTO batch_input VALUES ('B-506', 'L-205', 15);
INSERT INTO batch_input VALUES ('B-506', 'L-202', 10);
INSERT INTO lot VALUES ('L-303', 'I20', 40, DATE '2026-09-17', 'OK');
INSERT INTO produced_lot VALUES ('L-303', 'B-506');

-- ---------- Quality tests ----------
INSERT INTO quality_test VALUES ('L-091', 1, DATE '2026-08-26', 'Moisture',        'PASS', 'E11');
INSERT INTO quality_test VALUES ('L-095', 1, DATE '2026-08-31', 'Foreign matter',  'FAIL', 'E11');
INSERT INTO quality_test VALUES ('L-101', 1, DATE '2026-09-08', 'Moisture',        'PASS', 'E11');
INSERT INTO quality_test VALUES ('L-120', 1, DATE '2026-09-10', 'Microbial count', 'PASS', 'E11');
INSERT INTO quality_test VALUES ('L-205', 1, DATE '2026-09-12', 'Microbial count', 'PASS', 'E11');
INSERT INTO quality_test VALUES ('L-301', 1, DATE '2026-09-14', 'Pack seal',       'PASS', 'E11');
INSERT INTO quality_test VALUES ('L-302', 1, DATE '2026-09-17', 'Pack seal',       'PASS', 'E11');

-- ---------- Shipments ----------
INSERT INTO shipment VALUES ('S-80', 'C02', DATE '2026-08-30', 'TN23AB1180');
INSERT INTO shipment_line VALUES ('S-80', 'L-290', 300);

INSERT INTO shipment VALUES ('S-88', 'C01', DATE '2026-09-15', 'TN23AB4521');
INSERT INTO shipment_line VALUES ('S-88', 'L-301', 250);
INSERT INTO shipment VALUES ('S-89', 'C02', DATE '2026-09-16', 'TN23CD1209');
INSERT INTO shipment_line VALUES ('S-89', 'L-301', 150);
INSERT INTO shipment VALUES ('S-90', 'C03', DATE '2026-09-18', 'KA01EF7734');
INSERT INTO shipment_line VALUES ('S-90', 'L-302', 400);
INSERT INTO shipment_line VALUES ('S-90', 'L-303', 40);
INSERT INTO shipment VALUES ('S-91', 'C01', DATE '2026-09-19', 'TN23AB4521');
INSERT INTO shipment_line VALUES ('S-91', 'L-302', 200);
INSERT INTO shipment VALUES ('S-92', 'C04', DATE '2026-09-20', 'TN23GH0081');
INSERT INTO shipment_line VALUES ('S-92', 'L-301', 100);

-- ---------- A closed recall from August ----------
INSERT INTO recall VALUES ('R-001', 'L-095', 'E11', DATE '2026-08-31',
                           'Foreign particles found in sugar lot', 'HIGH', 'CLOSED');
INSERT INTO recall_lot VALUES ('R-001', 'L-190', 'DOWNSTREAM', 'DESTROYED');
INSERT INTO recall_lot VALUES ('R-001', 'L-290', 'DOWNSTREAM', 'RETRIEVE');

COMMIT;
