ALTER TABLE interventions ADD COLUMN cancellation_reason TEXT;

CREATE TABLE intervention_comments (
    id              UUID PRIMARY KEY,
    intervention_id UUID NOT NULL REFERENCES interventions(id),
    author_id       UUID NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_intervention ON intervention_comments(intervention_id);
CREATE INDEX idx_comments_author ON intervention_comments(author_id);
