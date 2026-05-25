# PyroSense Reporting Service

## Responsibility
Report generation for compliance, risk summaries, and maintenance history.

## Bounded Context
Reporting & Compliance

## Key Features
- Compliance reports (NF C 15-100)
- Risk summary reports per installation
- Maintenance history reports
- PDF generation
- TimescaleDB continuous aggregate queries

## Port
8088

## Dependencies
- PostgreSQL (report metadata)
- TimescaleDB (signal aggregates)
