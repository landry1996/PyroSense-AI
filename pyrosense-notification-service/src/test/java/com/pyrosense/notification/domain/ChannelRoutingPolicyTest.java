package com.pyrosense.notification.domain;

import com.pyrosense.notification.domain.model.ChannelRoutingPolicy;
import com.pyrosense.notification.domain.model.NotificationChannel;
import com.pyrosense.notification.domain.model.RecipientType;
import com.pyrosense.shared.valueobject.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChannelRoutingPolicyTest {

    @Test
    void infoShouldRouteToDashboardOnly() {
        Set<NotificationChannel> channels = ChannelRoutingPolicy.channelsFor(AlertSeverity.INFO);
        assertEquals(Set.of(NotificationChannel.DASHBOARD), channels);
    }

    @Test
    void warningShouldRouteToEmailAndDashboard() {
        Set<NotificationChannel> channels = ChannelRoutingPolicy.channelsFor(AlertSeverity.WARNING);
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.DASHBOARD));
        assertEquals(2, channels.size());
    }

    @Test
    void criticalShouldRouteToAllMainChannels() {
        Set<NotificationChannel> channels = ChannelRoutingPolicy.channelsFor(AlertSeverity.CRITICAL);
        assertTrue(channels.contains(NotificationChannel.SMS));
        assertTrue(channels.contains(NotificationChannel.PUSH));
        assertTrue(channels.contains(NotificationChannel.EMAIL));
        assertTrue(channels.contains(NotificationChannel.DASHBOARD));
        assertEquals(4, channels.size());
    }

    @Test
    void infoShouldNotifyManagersAndAdmins() {
        List<RecipientType> types = ChannelRoutingPolicy.recipientTypesFor(AlertSeverity.INFO);
        assertTrue(types.contains(RecipientType.PROPERTY_MANAGER));
        assertTrue(types.contains(RecipientType.TENANT_ADMIN));
        assertFalse(types.contains(RecipientType.OCCUPANT));
        assertFalse(types.contains(RecipientType.ELECTRICIAN));
    }

    @Test
    void warningShouldIncludeOccupant() {
        List<RecipientType> types = ChannelRoutingPolicy.recipientTypesFor(AlertSeverity.WARNING);
        assertTrue(types.contains(RecipientType.OCCUPANT));
        assertTrue(types.contains(RecipientType.PROPERTY_MANAGER));
        assertTrue(types.contains(RecipientType.TENANT_ADMIN));
    }

    @Test
    void criticalShouldIncludeElectrician() {
        List<RecipientType> types = ChannelRoutingPolicy.recipientTypesFor(AlertSeverity.CRITICAL);
        assertTrue(types.contains(RecipientType.ELECTRICIAN));
        assertTrue(types.contains(RecipientType.OCCUPANT));
        assertTrue(types.contains(RecipientType.PROPERTY_MANAGER));
        assertTrue(types.contains(RecipientType.TENANT_ADMIN));
    }
}
