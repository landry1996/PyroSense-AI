package com.pyrosense.notification.adapter.out.resolver;

import com.pyrosense.notification.application.port.out.RecipientResolverPort;
import com.pyrosense.notification.domain.model.Recipient;
import com.pyrosense.notification.domain.model.RecipientType;
import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class StubRecipientResolver implements RecipientResolverPort {

    @Override
    public List<Recipient> resolve(TenantId tenantId, List<RecipientType> types) {
        List<Recipient> recipients = new ArrayList<>();

        for (RecipientType type : types) {
            recipients.add(new Recipient(
                    new UserId(UUID.nameUUIDFromBytes((tenantId.value() + ":" + type.name()).getBytes())),
                    tenantId,
                    type,
                    "%s@pyrosense-tenant.local".formatted(type.name().toLowerCase()),
                    "+33600000000",
                    "push-token-" + type.name().toLowerCase(),
                    true, true, true
            ));
        }

        return recipients;
    }
}
