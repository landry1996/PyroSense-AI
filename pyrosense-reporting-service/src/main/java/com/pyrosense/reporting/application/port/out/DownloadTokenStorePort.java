package com.pyrosense.reporting.application.port.out;

import com.pyrosense.reporting.domain.model.DownloadToken;

import java.util.Optional;
import java.util.UUID;

public interface DownloadTokenStorePort {

    void store(DownloadToken token);

    Optional<DownloadToken> findByToken(String token);

    void invalidate(String token);
}
