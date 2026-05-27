package com.pyrosense.ingestion.adapter.out.redis;

import com.pyrosense.ingestion.adapter.in.mqtt.protocol.AntiReplayGuard;
import com.pyrosense.ingestion.adapter.in.mqtt.protocol.MqttProtocolConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisAntiReplayStores {

    private static final String NONCE_PREFIX = "ingestion:nonce:";
    private static final String SEQUENCE_PREFIX = "ingestion:seq:";
    private static final String MSGID_PREFIX = "ingestion:msgid:";
    private static final Duration NONCE_TTL = Duration.ofSeconds(MqttProtocolConstants.NONCE_TTL_SECONDS);
    private static final Duration MSGID_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public RedisAntiReplayStores(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Component
    public class RedisNonceStore implements AntiReplayGuard.NonceStore {
        @Override
        public boolean exists(String nonce) {
            return Boolean.TRUE.equals(redis.hasKey(NONCE_PREFIX + nonce));
        }

        @Override
        public void store(String nonce) {
            redis.opsForValue().set(NONCE_PREFIX + nonce, "1", NONCE_TTL);
        }
    }

    @Component
    public class RedisSequenceStore implements AntiReplayGuard.SequenceStore {
        @Override
        public long getLastSequence(String deviceId) {
            String val = redis.opsForValue().get(SEQUENCE_PREFIX + deviceId);
            return val != null ? Long.parseLong(val) : 0;
        }

        @Override
        public void updateSequence(String deviceId, long sequence) {
            redis.opsForValue().set(SEQUENCE_PREFIX + deviceId, String.valueOf(sequence));
        }
    }

    @Component
    public class RedisMessageIdStore implements AntiReplayGuard.MessageIdStore {
        @Override
        public boolean exists(String messageId) {
            return Boolean.TRUE.equals(redis.hasKey(MSGID_PREFIX + messageId));
        }

        @Override
        public void store(String messageId) {
            redis.opsForValue().set(MSGID_PREFIX + messageId, "1", MSGID_TTL);
        }
    }
}
