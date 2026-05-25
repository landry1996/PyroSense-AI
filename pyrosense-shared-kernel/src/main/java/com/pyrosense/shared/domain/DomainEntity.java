package com.pyrosense.shared.domain;

/**
 * Marker interface for domain entities (non-root entities within an aggregate).
 * Entities have identity but are managed by their aggregate root.
 */
public interface DomainEntity<ID> {

    ID getId();
}
