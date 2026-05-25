package com.pyrosense.notification.domain.model;

import com.pyrosense.shared.id.TenantId;
import com.pyrosense.shared.id.UserId;

import java.util.Objects;

public record Recipient(
        UserId userId,
        TenantId tenantId,
        RecipientType type,
        String email,
        String phone,
        String pushToken,
        boolean consentEmail,
        boolean consentSms,
        boolean consentPush
) {
    public Recipient {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(type);
    }

    public boolean canReceive(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> consentEmail && email != null && !email.isBlank();
            case SMS -> consentSms && phone != null && !phone.isBlank();
            case PUSH -> consentPush && pushToken != null && !pushToken.isBlank();
            case WEBHOOK -> true;
            case DASHBOARD -> true;
        };
    }

    public String maskedPhone() {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }

    public String maskedEmail() {
        if (email == null || !email.contains("@")) return "***";
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
