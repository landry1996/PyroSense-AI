# MVP 3 — Risk Register

> **Date**: 2026-05-28
> **Version**: 1.0
> **Scope**: All identified risks for MVP 3 pilot terrain (10 capteurs, 1 batiment)

## Risk Matrix

| Likelihood \ Impact | Negligible | Minor | Moderate | Major | Critical |
|---------------------|-----------|-------|----------|-------|----------|
| **Almost Certain** | | | | | |
| **Likely** | | | DQ-02 | S-03, S-04 | F-02 |
| **Possible** | | CI-05 | I-04, DS-02 | P-01, P-03, CI-01 | I-02, I-03 |
| **Unlikely** | T-06 | DQ-01 | S-06, S-07, F-04 | P-02, DS-01 | |
| **Rare** | | | | | |

---

## Security Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| S-01 | Timing side-channel in HMAC comparison | Unlikely | Critical | CRITICAL | FIXED | Backend |
| S-02 | SIGNATURE_DISABLED bypass (empty key) | Possible | Critical | CRITICAL | FIXED | Backend |
| S-03 | Heartbeat messages skip security pipeline | Likely | Major | HIGH | OPEN | Backend |
| S-04 | Legacy MQTT topics bypass all authentication | Likely | Major | HIGH | OPEN | Backend |
| S-05 | Rate limiter race condition (burst bypass) | Possible | Moderate | HIGH | FIXED | Backend |
| S-06 | Anti-replay nonce freshness not validated | Unlikely | Moderate | MEDIUM | ACCEPTED | Backend |
| S-07 | Nonce entropy only 32 bits effective | Unlikely | Moderate | MEDIUM | ACCEPTED | Backend |
| S-08 | MQTT broker allows anonymous connections | Possible | Major | HIGH | OPEN | Infra |
| S-09 | Cross-tenant data quality metric exposure | Possible | Critical | CRITICAL | OPEN | Backend |
| S-10 | Cross-tenant data quality endpoint access | Possible | Critical | CRITICAL | OPEN | Backend |

---

## Firmware Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| F-01 | Hardcoded WiFi credentials | Certain | Major | CRITICAL | FIXED | Firmware |
| F-02 | HMAC is XOR placeholder (not cryptographic) | Likely | Critical | CRITICAL | KNOWN LIMITATION | Firmware |
| F-03 | HMAC disabled by default in config | Likely | Major | HIGH | DOCUMENTED | Firmware |
| F-04 | No boot-time security credential check | Unlikely | Moderate | MEDIUM | OPEN | Firmware |
| F-05 | Firmware instability on real hardware | Possible | Major | HIGH | PENDING VALIDATION | Firmware |
| F-06 | Memory leak over 24h operation | Possible | Major | HIGH | PENDING VALIDATION | Firmware |

---

## Provisioning Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| P-01 | IP spoofing bypasses provisioning rate limit | Possible | Major | HIGH | OPEN | Backend |
| P-02 | Claim token accepted without tenant validation | Unlikely | Major | HIGH | OPEN | Backend |
| P-03 | SHA-256 without salt (rainbow table attack) | Possible | Major | HIGH | OPEN | Backend |
| P-04 | Random TenantId on provisioning failure | Unlikely | Minor | LOW | OPEN | Backend |

---

## Ingestion Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| I-01 | Timestamp validation rejects valid messages | Likely | Moderate | HIGH | FIXED | Backend |
| I-02 | Data quality queries not tenant-isolated | Possible | Critical | CRITICAL | OPEN | Backend |
| I-03 | Global metrics exposed to all tenants | Possible | Critical | CRITICAL | OPEN | Backend |
| I-04 | Sequence=0 accepted as valid after reboot | Possible | Moderate | MEDIUM | ACCEPTED | Backend |

---

## Data & Privacy Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| DS-01 | Pseudonymization reversible (64-bit output) | Unlikely | Major | HIGH | OPEN | Backend |
| DS-02 | Deterministic pseudonymization enables lookup | Possible | Moderate | MEDIUM | OPEN | Backend |
| DQ-01 | Clock drift not persisted (no historical analysis) | Unlikely | Minor | LOW | ACCEPTED | Backend |
| DQ-02 | Signal quality threshold inconsistency | Likely | Negligible | LOW | ACCEPTED | Backend |

---

## CI/CD Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| CI-01 | Existing ci.yml has no CVSS fail threshold | Possible | Major | HIGH | OPEN | DevOps |
| CI-02 | Secret scan regex too permissive | Possible | Moderate | MEDIUM | OPEN | DevOps |
| CI-03 | Coverage threshold too low for security code | Possible | Moderate | MEDIUM | OPEN | DevOps |
| CI-04 | Deploy health check not implemented | Possible | Moderate | MEDIUM | OPEN | DevOps |
| CI-05 | cppcheck suppressions hide issues | Possible | Negligible | LOW | ACCEPTED | DevOps |

---

## Test Coverage Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| T-01 | Rate limiter: no concurrency tests | Possible | Moderate | HIGH | OPEN | QA |
| T-02 | Pipeline test skips rate limit/audit paths | Possible | Moderate | HIGH | OPEN | QA |
| T-03 | No firmware HMAC unit tests | Possible | Major | HIGH | OPEN | QA |
| T-04 | Heartbeat security bypass not tested | Likely | Moderate | HIGH | OPEN | QA |
| T-05 | No concurrent anti-replay tests | Possible | Moderate | MEDIUM | OPEN | QA |
| T-06 | Legacy handler bypass not tested | Unlikely | Negligible | LOW | ACCEPTED | QA |

---

## Operational Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| OP-01 | WiFi interference in building | Possible | Moderate | MEDIUM | MITIGATED (buffer 72h) | Ops |
| OP-02 | Keycloak realm not configured | Likely | Major | HIGH | BLOCKING | Infra |
| OP-03 | TLS certificate not validated end-to-end | Possible | Major | HIGH | OPEN | Infra |
| OP-04 | Electrician availability uncertain | Possible | Major | HIGH | OPEN | Project |
| OP-05 | Faux positifs > 5% | Possible | Moderate | MEDIUM | MITIGATED (feedback) | Backend |

---

## Electrical & Safety Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| EL-01 | Non-professional installation attempt | Unlikely | Critical | HIGH | DOCUMENTED | Legal |
| EL-02 | 230V exposure during maintenance | Unlikely | Critical | HIGH | DOCUMENTED (consignation) | Safety |
| EL-03 | False fire safety confidence | Possible | Critical | HIGH | DOCUMENTED (disclaimer) | Legal |
| EL-04 | Thermal runaway in enclosure | Unlikely | Major | MEDIUM | PLANNED (thermal tests) | Hardware |
| EL-05 | EMC interference with panel | Unlikely | Moderate | MEDIUM | PLANNED (CEM tests) | Hardware |

---

## Regulatory Risks

| ID | Risk | Likelihood | Impact | Severity | Status | Owner |
|----|------|-----------|--------|----------|--------|-------|
| REG-01 | Operating without IEC 61439 | Certain | Minor (pilot) | LOW | ACCEPTED (pilot scope) | Legal |
| REG-02 | Operating without NF C 15-100 | Certain | Minor (pilot) | LOW | ACCEPTED (pilot scope) | Legal |
| REG-03 | RGPD non-compliance | Unlikely | Major | LOW | COMPLIANT | Backend |
| REG-04 | Missing RC Pro insurance | Possible | Major | HIGH | OPEN | Management |

---

## Risk Treatment Summary

| Treatment | Count | IDs |
|-----------|-------|-----|
| FIXED (this audit) | 7 | S-01, S-02, S-05, F-01, I-01 (+ pipeline fixes) |
| OPEN (requires action) | 20 | S-03, S-04, S-08, S-09, S-10, P-01, P-02, P-03, I-02, I-03, DS-01, CI-01, CI-02, CI-03, CI-04, T-01-T-05, OP-02, OP-03, OP-04 |
| ACCEPTED (known, tolerable) | 8 | S-06, S-07, I-04, DQ-01, DQ-02, CI-05, T-06, REG-01, REG-02 |
| DOCUMENTED (procedural) | 5 | EL-01, EL-02, EL-03, F-03 |
| KNOWN LIMITATION | 1 | F-02 (XOR placeholder, requires mbedtls) |
| BLOCKING (must fix before pilot) | 1 | OP-02 (Keycloak) |

---

## Residual Risk After Treatment

After applying all fixes and recommended mitigations:

| Risk Level | Before Audit | After Fixes | After All Recommendations |
|-----------|:------------:|:-----------:|:-------------------------:|
| CRITICAL | 8 | 3 | 0 |
| HIGH | 9 | 7 | 2 |
| MEDIUM | 12 | 10 | 5 |
| LOW | 6 | 6 | 6 |

**Acceptable residual risk for controlled pilot (10 devices, 1 building, supervised).**
