#pragma once

// ============================================================================
// PyroSense Device Configuration Template
// ============================================================================
// Copy this file to config.h and fill in your values.
// config.h is in .gitignore — NEVER commit secrets.
// ============================================================================

// --- Device Identity ---
#define PYRO_DEVICE_ID        "pyro-dev-001"
#define PYRO_TENANT_ID        "tenant-lab-01"

// --- WiFi ---
#define PYRO_WIFI_SSID        "your-wifi-ssid"
#define PYRO_WIFI_PASSWORD    "your-wifi-password"

// --- MQTT Broker ---
#define PYRO_MQTT_BROKER_URI  "mqtt://192.168.1.100"
#define PYRO_MQTT_PORT        1883
#define PYRO_MQTT_USERNAME    ""
#define PYRO_MQTT_PASSWORD    ""
#define PYRO_MQTT_USE_TLS     false

// --- Security ---
// HMAC-SHA256 key (hex string, 64 chars = 32 bytes)
// Generate with: openssl rand -hex 32
#define PYRO_HMAC_KEY         "0000000000000000000000000000000000000000000000000000000000000000"
#define PYRO_HMAC_ENABLED     false

// --- NTP ---
#define PYRO_NTP_SERVER       "pool.ntp.org"
