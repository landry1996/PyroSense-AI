package com.pyrosense.maintenance.adapter.out.stub;

import com.pyrosense.maintenance.application.port.out.AlertLookupPort;
import com.pyrosense.shared.id.AlertId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StubAlertLookupAdapter implements AlertLookupPort {

    @Override
    public Optional<AlertInfo> findById(AlertId alertId) {
        return Optional.empty();
    }
}
