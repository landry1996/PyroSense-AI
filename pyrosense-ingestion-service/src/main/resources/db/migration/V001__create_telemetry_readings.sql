-- V001: Enable TimescaleDB extension (idempotent)
-- TimescaleDB must be installed on the PostgreSQL server.
-- If not available, tables work as regular partitioned tables.
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
