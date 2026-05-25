# PyroSense: A Non-Technical Guide

## What is PyroSense?

> **PyroSense is a doctor for your building's electrical system — it listens to the heartbeat of your wires and warns you before a fire starts.**

### The Problem

- **25% of building fires are electrical in origin**
- Most electrical fires are caused by slow degradation: a connection loosens over months, generates heat, and one day ignites surrounding material
- These faults produce early warning signs — tiny sparks, unusual heat, electrical noise — weeks or months before a fire
- Today, nobody is listening for those signs. Inspections happen once a year at best.

### The Solution

Small sensors installed on electrical panels that continuously monitor electrical signals 24/7 and alert building managers when something starts going wrong — long before it becomes dangerous.

---

## The Doctor Analogy

Think of PyroSense as a cardiologist for your building:

| Medical World | PyroSense Equivalent |
|---------------|---------------------|
| Stethoscope | Sensor on the electrical panel |
| Your normal heart rate | Baseline (what is normal for YOUR building) |
| Irregular heartbeat | Anomaly (something changed in the signal) |
| Health score (blood pressure, cholesterol...) | Risk Score (0 = healthy, 100 = emergency) |
| Doctor's phone call | Alert ("You should schedule an inspection") |
| Surgery | Electrician intervention (fix the problem) |

Just as a heart monitor learns YOUR normal resting heart rate and alerts when something deviates, PyroSense learns what YOUR electrical system normally looks like and alerts when something changes.

---

## Journey of a Data Point: From Sensor to Alert

Here is what happens when your building's electrical system starts developing a problem:

### Step 1: Measurement (continuous, every second)
The sensor measures 10 electrical parameters simultaneously:
- Current (how much electricity is flowing)
- Voltage (electrical pressure)
- Power (energy consumption)
- Harmonics (electrical noise/distortion)
- Temperature (heat at the connection point)
- Micro-arcs (tiny sparks invisible to the naked eye)
- And more...

### Step 2: Transmission (~100 milliseconds)
Data travels from sensor to the platform via MQTT (a lightweight messaging protocol designed for IoT devices). This happens in real-time — less than a tenth of a second.

### Step 3: Ingestion (immediate)
The platform receives the data and performs three checks:
- **Validation**: Is this reading sensible? (A temperature of 5000 degrees is clearly a sensor error)
- **Deduplication**: Have we already seen this exact reading? (Avoids counting the same data twice)
- **Storage**: Save it for historical analysis and baseline learning

### Step 4: Analysis (~1 second)
The platform compares the new reading against the baseline profile:
- "Is this normal for this specific device?"
- "Is this normal for this time of day/week/season?"
- "Has this pattern been appearing more often?"

### Step 5: Scoring (~500 milliseconds)
If an anomaly is detected, the platform calculates an overall risk score considering:
- How severe is the deviation?
- How often has it happened before?
- Is it getting worse over time?

### Step 6: Alert (if score exceeds threshold)
If the risk score crosses a danger threshold, an alert is created:
- **INFO**: Minor observation, logged for reference
- **WARNING**: Needs attention within days
- **CRITICAL**: Needs attention within hours

### Step 7: Notification (<5 seconds from alert creation)
The responsible person receives an SMS, email, or push notification with:
- Which building and panel
- What was detected
- How urgent it is
- Recommended action

### Total Time: From electrical fault to human notification — less than 10 seconds.

---

## Understanding the Risk Score

The risk score is a number from 0 to 100 that represents the overall health of an electrical installation.

### Score Ranges

| Score | Level | Meaning | Action |
|-------|-------|---------|--------|
| 0-20 | LOW | Everything looks normal | No action needed |
| 21-50 | MODERATE | Minor irregularities detected | Monitor, be aware |
| 51-75 | HIGH | Significant anomaly detected | Schedule electrician inspection |
| 76-100 | CRITICAL | Imminent danger indicators | Act immediately |

### What Goes Into the Score

Think of it as a report card with six subjects:

| Factor | Weight | What It Measures |
|--------|--------|-----------------|
| Micro-arc signals | 30% | Tiny sparks inside wire connections — the #1 indicator of impending failure |
| Harmonic distortion | 20% | Electrical noise that indicates wiring problems |
| Temperature | 20% | Overheating at connection points |
| Transients | 10% | Sudden voltage spikes |
| High-frequency noise | 10% | Subtle interference patterns |
| Device reliability history | 10% | How trustworthy this device's readings are |

### Example

> "Your panel's risk score is 67 (HIGH).
> Main contributors: Micro-arc signals detected repeatedly (28 points), Temperature trending upward for 3 days (18 points), Harmonic distortion above normal (12 points).
> Recommendation: Schedule an electrician visit this week."

---

## Understanding Alerts

### Severity Levels

**INFO** — "FYI, we noticed something. No action needed yet."
- Example: "Device B3 saw a brief voltage spike at 3 AM. It has not recurred."

**WARNING** — "Something is degrading. Schedule an electrician visit within days."
- Example: "Connection temperature on Panel A2 has been rising steadily for 5 days."

**CRITICAL** — "Potential danger. Act within hours."
- Example: "Repeated micro-arc events detected on Circuit 7. Pattern consistent with insulation breakdown."

### Alert Lifecycle

1. **Created** — System detected the problem
2. **Acknowledged** — Human saw the alert
3. **Assigned** — Electrician scheduled
4. **Resolved** — Problem fixed (or confirmed as False Positive)

### Smart Deduplication

If the same problem persists, PyroSense does NOT send a new alert every second. One alert is created and updated until the problem is resolved. No spam.

---

## The ROI Argument

### Cost of Doing Nothing

| Consequence | Typical Cost |
|-------------|-------------|
| Electrical fire (property damage) | 50,000 - 500,000+ EUR |
| Business interruption | 10,000 - 100,000+ EUR per day |
| Liability claims | Variable, potentially millions |
| Human cost | Incalculable |

### Cost of PyroSense

- Sensor hardware per panel: one-time installation cost
- Platform subscription per building: monthly fee
- Total cost of ownership: a fraction of one prevented incident

### Financial Benefits

| Benefit | Estimated Value |
|---------|----------------|
| Insurance premium reduction | 10-30% with certified continuous monitoring |
| Maintenance savings | Replace reactive (wait for failure) with predictive (fix before failure) |
| Regulatory compliance | Meets upcoming EU building safety monitoring requirements |
| Break-even | One prevented incident typically pays for 5-10 years of monitoring |

### The Simple Math

If PyroSense prevents even ONE electrical incident in a building's lifetime, it has paid for itself many times over. The question is not "can we afford PyroSense?" but "can we afford NOT to have it?"

---

## What PyroSense Does NOT Do (Honest Limitations)

Transparency builds trust. Here is what PyroSense cannot do:

| What it does NOT do | Why |
|---------------------|-----|
| Replace electrical code compliance (NF C 15-100) | Monitoring is a complement to proper installation, not a substitute |
| Replace annual mandatory inspections | Inspections verify physical condition that sensors cannot see |
| Physically disconnect power | No circuit breaker control — PyroSense is monitoring only, not actuation |
| Guarantee 100% detection | No system is perfect. Novel failure modes may not match known patterns |
| Work without sensors installed | This is a software MVP — hardware sensors are not yet manufactured |
| Provide certified safety ratings | Real-world validation with actual electrical faults is still needed |

### Current Status

PyroSense is a **software MVP** (Minimum Viable Product). The detection algorithms are built and running, but they are currently processing **simulated data**, not real sensor data. Real-world validation with pilot installations is the next step.

---

## Glossary for Non-Technical Readers

| Term | Simple Explanation |
|------|-------------------|
| **Micro-arc** | A tiny electrical spark inside a wire connection. Invisible and inaudible to humans, but detectable by sensors. Like a match being struck inside your wall — once is nothing, repeatedly is a fire risk. |
| **THD (Total Harmonic Distortion)** | Electrical noise that indicates problems. Imagine listening to a pure musical note — THD is the buzzing and distortion that creeps in when the instrument is damaged. |
| **Baseline** | What "normal" looks like for each specific installation. Your building at 2 PM on a Tuesday has a different electrical profile than at 3 AM on a Sunday. The baseline captures all these normal patterns. |
| **Anomaly** | Something that deviates from the baseline. Not necessarily dangerous — just different from what was expected. Could be a new appliance, or could be a developing fault. |
| **Tenant** | An organization (building management company, property owner) using the platform. Each tenant manages their own buildings and sees only their own data. |
| **MVP (Minimum Viable Product)** | The first working version of a product — enough to demonstrate the concept and gather feedback, but not yet the final polished product. |
| **MQTT** | A lightweight communication protocol used by IoT devices. Think of it as a postal service optimized for tiny, frequent messages — perfect for sensors sending data every second. |
| **IoT (Internet of Things)** | Physical devices (sensors, cameras, thermostats) connected to the internet. In our case: electrical sensors that send data to the cloud. |
| **Hexagonal Architecture** | A software design pattern where the business logic is isolated from external systems. Like building a house where you can change the plumbing without rebuilding the walls. |
| **Z-Score** | How many "standard deviations" a measurement is from the average. A Z-score of 3 means "this value is very unusual — it happens less than 1% of the time under normal conditions." |
| **EMA (Exponential Moving Average)** | A smoothing technique that gives more weight to recent data. Like asking "what is the trend?" while ignoring momentary blips. |
| **Risk Score** | A number from 0 to 100 representing the health of an electrical installation. Combines multiple factors into one actionable number. |
| **Alert** | A notification generated when the risk score exceeds a threshold. Contains severity, location, cause, and recommended action. |
| **Welford's Algorithm** | A mathematical method for computing averages and variability in a stream of data without storing all past values. Efficient and accurate. |
| **Federated Learning** | A way to train AI models across multiple organizations without sharing raw data. Each organization keeps its data private while contributing to a shared model. |
