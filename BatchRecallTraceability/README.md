# Batch Recall Traceability — Phase 2 (Implementation Core)

Oracle database + Java Swing front end connected with JDBC.

```
sql/00_create_user.sql   create the application user (run once as SYSTEM)
sql/00_drop.sql          drop everything (safe on an empty schema)
sql/01_schema.sql        DDL: 15 tables, constraints, 2 views, 1 sequence
sql/02_sample_data.sql   DML: sample data, 3-level lot genealogy, 1 closed recall
sql/03_queries.sql       stock report, downstream/upstream traces, recall summary
src/trace/               Java source (JDBC, business rules, Swing screens)
db.properties            connection settings
run.bat / run.sh         compile and start the app
lib/                     put the Oracle JDBC driver here (ojdbc8.jar or ojdbc11.jar)
```

## 1. Database (SQL*Plus)

```
sqlplus system/<password>
SQL> @sql/00_create_user.sql
SQL> connect trace_app/trace_app
SQL> @sql/00_drop.sql
SQL> @sql/01_schema.sql
SQL> @sql/02_sample_data.sql
SQL> @sql/03_queries.sql
```

On Oracle 12c and later, connect to the pluggable database first
(for example `sqlplus system/<password>@localhost:1521/XEPDB1`).

## 2. JDBC driver

Copy the Oracle JDBC driver into `lib/`:
- from the Oracle installation: `<ORACLE_HOME>/jdbc/lib/ojdbc8.jar` (or `ojdbc11.jar`), or
- download it from oracle.com (Database > JDBC drivers).

## 3. Connection settings

Edit `db.properties`:

```
db.url=jdbc:oracle:thin:@localhost:1521:XE        # Oracle XE 11g / 18c, using the SID
# db.url=jdbc:oracle:thin:@localhost:1521/XEPDB1  # Oracle 18c / 21c pluggable database
db.user=trace_app
db.password=trace_app
```

## 4. Run

Windows: double-click `run.bat`  ·  Linux / macOS: `sh run.sh`

The status bar at the bottom of the window shows the database and driver the app is connected to.

## Screens

| Tab | User | What it does |
|---|---|---|
| Lots and stock | everyone | all lots with status, stock left and expiry (views LOT_STOCK, LOT_EXPIRY) |
| Receive raw lot | stores keeper | inserts LOT + RAW_LOT |
| Production batch | production supervisor | inserts PRODUCTION_BATCH + BATCH_INPUT + LOT + PRODUCED_LOT |
| Quality test | QA officer | inserts QUALITY_TEST; a FAIL puts the lot on HOLD |
| Shipment | dispatch clerk | inserts SHIPMENT + SHIPMENT_LINE |
| Trace and recall | QA officer | recursive upstream/downstream trace; raises a recall |

## Business rules (TraceService.java)

1. A raw lot must be a RAW item, quantity > 0, not received in the future, made before it was received; lot numbers are unique.
2. A batch needs at least one input; each input lot must be OK (not HOLD/RECALLED), not finished goods, not expired on the batch date, and have enough stock left; the output must be an intermediate or finished item.
3. A failed quality test puts an OK lot on HOLD.
4. Only finished, OK, unexpired lots can be shipped, and never more than the stock left.
5. A recall marks the defective lot and every lot made from it RECALLED, puts every lot it was made from on HOLD for investigation, records all of them in RECALL_LOT, and lists the shipments to retrieve. Only one open recall per lot.

Every save runs in one transaction (auto-commit off): all rows are written or none are.
All SQL uses PreparedStatement parameters.

## Testing note

The scripts and the app were tested on the H2 database in Oracle-compatibility mode
(23 business-rule tests passing). Run the scripts once on your lab Oracle before the review.
The `CONNECT BY` query in `03_queries.sql` is Oracle-only.
