-- Run once as SYSTEM (on 12c and later, inside the pluggable database, e.g. XEPDB1).
CREATE USER trace_app IDENTIFIED BY trace_app;
GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE TO trace_app;
ALTER USER trace_app QUOTA UNLIMITED ON USERS;
