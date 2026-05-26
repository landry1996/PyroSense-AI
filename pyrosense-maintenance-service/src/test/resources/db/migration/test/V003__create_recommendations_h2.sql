CREATE TABLE intervention_recommendations (
    id                          UUID PRIMARY KEY,
    tenant_id                   UUID NOT NULL,
    alert_id                    UUID NOT NULL,
    device_id                   UUID NOT NULL,
    suggested_type              VARCHAR(20) NOT NULL,
    suggested_priority          VARCHAR(10) NOT NULL,
    reason                      TEXT NOT NULL,
    sla_deadline_seconds        BIGINT NOT NULL,
    sla_expires_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    status                      VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    rejection_reason            TEXT,
    accepted_intervention_id    UUID,
    decided_at                  TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommendation_intervention FOREIGN KEY (accepted_intervention_id) REFERENCES interventions(id)
);

CREATE INDEX idx_recommendations_tenant_status ON intervention_recommendations(tenant_id, status);
CREATE UNIQUE INDEX idx_recommendations_alert ON intervention_recommendations(alert_id);
