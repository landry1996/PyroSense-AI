package com.pyrosense.ingestion.config;

import com.pyrosense.ingestion.adapter.in.mqtt.protocol.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProtocolValidationConfig {

    @Bean
    public PayloadValidator payloadValidator() {
        return new PayloadValidator();
    }

    @Bean
    public SignatureVerifier signatureVerifier() {
        return new SignatureVerifier();
    }

    @Bean
    public AntiReplayGuard antiReplayGuard(
            AntiReplayGuard.NonceStore nonceStore,
            AntiReplayGuard.SequenceStore sequenceStore,
            AntiReplayGuard.MessageIdStore messageIdStore) {
        return new AntiReplayGuard(nonceStore, sequenceStore, messageIdStore);
    }

    @Bean
    public DeviceRateLimiter deviceRateLimiter(
            @Value("${pyrosense.security.device-rate-limit.max-messages:120}") int maxMessages,
            @Value("${pyrosense.security.device-rate-limit.window-seconds:60}") int windowSeconds) {
        return new DeviceRateLimiter(maxMessages, windowSeconds);
    }

    @Bean
    public IoTSecurityAuditor ioTSecurityAuditor() {
        return new IoTSecurityAuditor();
    }

    @Bean
    public ProtocolValidationPipeline protocolValidationPipeline(
            PayloadValidator validator,
            SignatureVerifier signatureVerifier,
            AntiReplayGuard antiReplayGuard,
            ProtocolValidationPipeline.DeviceStatusChecker deviceStatusChecker,
            DeviceRateLimiter rateLimiter,
            IoTSecurityAuditor auditor) {
        return new ProtocolValidationPipeline(validator, signatureVerifier, antiReplayGuard,
                deviceStatusChecker, rateLimiter, auditor);
    }
}
