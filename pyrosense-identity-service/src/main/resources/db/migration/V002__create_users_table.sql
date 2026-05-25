CREATE TABLE users (
    id                      UUID PRIMARY KEY,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    full_name               VARCHAR(255) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    last_login_at           TIMESTAMPTZ,
    locked_until            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
