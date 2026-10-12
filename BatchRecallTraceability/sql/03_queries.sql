-- =====================================================================
-- Batch Recall Traceability : useful queries (Oracle)
-- Replace L-205 with any lot number.
-- =====================================================================

-- 1. Stock and expiry of every lot
SELECT s.lot_id, i.item_name, l.status, s.quantity, s.qty_used, s.qty_shipped,
       s.qty_available, e.expiry_date
FROM lot_stock s
JOIN lot l        ON l.lot_id = s.lot_id
JOIN item i       ON i.item_id = l.item_id
JOIN lot_expiry e ON e.lot_id = s.lot_id
ORDER BY s.lot_id;

-- 2. Downstream trace (forward): every lot made from L-205, at any depth
WITH down (lot_id, lvl) AS (
  SELECT lot_id, 0 FROM lot WHERE lot_id = 'L-205'
  UNION ALL
  SELECT pl.lot_id, d.lvl + 1
  FROM down d
  JOIN batch_input bi  ON bi.lot_id = d.lot_id
  JOIN produced_lot pl ON pl.batch_id = bi.batch_id
)
SELECT d.lot_id, MIN(d.lvl) AS steps_away, i.item_name, l.status
FROM down d
JOIN lot l  ON l.lot_id = d.lot_id
JOIN item i ON i.item_id = l.item_id
WHERE d.lvl > 0
GROUP BY d.lot_id, i.item_name, l.status
ORDER BY steps_away, d.lot_id;

-- 3. Shipments and customers that received L-205 or anything made from it
WITH down (lot_id, lvl) AS (
  SELECT lot_id, 0 FROM lot WHERE lot_id = 'L-205'
  UNION ALL
  SELECT pl.lot_id, d.lvl + 1
  FROM down d
  JOIN batch_input bi  ON bi.lot_id = d.lot_id
  JOIN produced_lot pl ON pl.batch_id = bi.batch_id
)
SELECT DISTINCT s.shipment_id, s.ship_date, c.customer_name, c.city, c.phone,
       sl.lot_id, sl.qty_shipped
FROM down d
JOIN shipment_line sl ON sl.lot_id = d.lot_id
JOIN shipment s       ON s.shipment_id = sl.shipment_id
JOIN customer c       ON c.customer_id = s.customer_id
ORDER BY s.ship_date, s.shipment_id;

-- 4. Upstream trace (backward): every input lot behind L-301, with its supplier
WITH up (lot_id, lvl) AS (
  SELECT lot_id, 0 FROM lot WHERE lot_id = 'L-301'
  UNION ALL
  SELECT bi.lot_id, u.lvl + 1
  FROM up u
  JOIN produced_lot pl ON pl.lot_id = u.lot_id
  JOIN batch_input bi  ON bi.batch_id = pl.batch_id
)
SELECT u.lot_id, MIN(u.lvl) AS steps_away, i.item_name, sp.supplier_name, r.invoice_no
FROM up u
JOIN lot l          ON l.lot_id = u.lot_id
JOIN item i         ON i.item_id = l.item_id
LEFT JOIN raw_lot r ON r.lot_id = u.lot_id
LEFT JOIN supplier sp ON sp.supplier_id = r.supplier_id
WHERE u.lvl > 0
GROUP BY u.lot_id, i.item_name, sp.supplier_name, r.invoice_no
ORDER BY steps_away, u.lot_id;

-- 5. The same downstream trace with Oracle's CONNECT BY syntax
SELECT child_lot, LEVEL AS steps_away
FROM (SELECT bi.lot_id AS parent_lot, pl.lot_id AS child_lot
      FROM batch_input bi JOIN produced_lot pl ON pl.batch_id = bi.batch_id)
START WITH parent_lot = 'L-205'
CONNECT BY NOCYCLE PRIOR child_lot = parent_lot;

-- 6. Recall summary
SELECT r.recall_id, r.trigger_lot_id, r.recall_date, r.severity, r.status,
       rl.lot_id, rl.direction, rl.action_taken
FROM recall r LEFT JOIN recall_lot rl ON rl.recall_id = r.recall_id
ORDER BY r.recall_id, rl.direction, rl.lot_id;
