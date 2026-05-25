package com.pyrosense.reporting.adapter.out.persistence;

import com.pyrosense.reporting.application.port.out.DownloadTokenStorePort;
import com.pyrosense.reporting.domain.model.DownloadToken;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryDownloadTokenStore implements DownloadTokenStorePort {

    private final Map<String, DownloadToken> store = new ConcurrentHashMap<>();

    @Override
    public void store(DownloadToken token) {
        store.put(token.token(), token);
    }

    @Override
    public Optional<DownloadToken> findByToken(String token) {
        return Optional.ofNullable(store.get(token));
    }

    @Override
    public void invalidate(String token) {
        store.remove(token);
    }
}
