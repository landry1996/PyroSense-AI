package com.pyrosense.ingestion.adapter.in.mqtt.protocol;

import java.util.Set;

public final class MqttProtocolConstants {

    private MqttProtocolConstants() {}

    public static final String TOPIC_PREFIX = "pyrosense/v1/";
    public static final String TOPIC_TELEMETRY = "telemetry";
    public static final String TOPIC_HEARTBEAT = "heartbeat";
    public static final String TOPIC_EVENTS = "events";
    public static final String TOPIC_PROVISIONING = "provisioning";
    public static final String TOPIC_COMMANDS = "commands";
    public static final String TOPIC_COMMAND_ACKS = "command-acks";

    public static final Set<String> SUPPORTED_SCHEMA_VERSIONS = Set.of("1.0");
    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    public static final int MAX_PAYLOAD_SIZE_BYTES = 8192;
    public static final int MAX_TIMESTAMP_FUTURE_SECONDS = 300;
    public static final int MAX_REALTIME_TIMESTAMP_AGE_SECONDS = 600; // 10min tolerance for real-time messages
    public static final int MAX_TIMESTAMP_AGE_SECONDS = 259200; // 72h for drain mode
    public static final int NONCE_LENGTH = 24;
    public static final int SIGNATURE_LENGTH = 64;
    public static final int NONCE_TTL_SECONDS = 86400; // 24h

    public static final String TOPIC_PATTERN_V1 = "pyrosense/v1/([^/]+)/([^/]+)/(telemetry|heartbeat|events|command-acks)";
    public static final String PROVISIONING_TOPIC_PATTERN = "pyrosense/v1/provisioning/([^/]+)";
}
