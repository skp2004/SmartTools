-- V2__smarttools_schema.sql
-- SmartTools SaaS schema — replaces the invoice SaaS tables.
-- Supports guest usage (user_id nullable on conversions/usage).

-- Drop all old invoice-era tables (safe — they're no longer needed)
DROP TABLE IF EXISTS invoice_items CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
DROP TABLE IF EXISTS companies CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =============================================
-- USERS
-- =============================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),                    -- nullable for OAuth users
    name            VARCHAR(255) NOT NULL,
    picture_url     VARCHAR(512),
    provider        VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
                    CHECK (provider IN ('LOCAL', 'GOOGLE')),
    provider_id     VARCHAR(255),                    -- Google sub / provider UID
    role            VARCHAR(10)  NOT NULL DEFAULT 'USER'
                    CHECK (role IN ('USER', 'ADMIN')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_provider ON users (provider, provider_id) WHERE provider_id IS NOT NULL;

-- =============================================
-- SUBSCRIPTIONS  (one per user)
-- =============================================
CREATE TABLE subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    plan            VARCHAR(10) NOT NULL DEFAULT 'FREE'
                    CHECK (plan IN ('FREE', 'PRO')),
    status          VARCHAR(15) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'CANCELLED')),
    renewal_date    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =============================================
-- CONVERSIONS  (one record per tool invocation)
-- user_id is nullable so guests can use tools
-- =============================================
CREATE TABLE conversions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    tool                VARCHAR(50)  NOT NULL,   -- e.g. 'pdf-merge', 'image-compress'
    status              VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED')),
    input_filename      VARCHAR(500),
    output_filename     VARCHAR(500),
    file_size_bytes     BIGINT,
    processing_time_ms  BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ              -- temp files deleted after this
);

CREATE INDEX idx_conversions_user   ON conversions (user_id);
CREATE INDEX idx_conversions_tool   ON conversions (tool);
CREATE INDEX idx_conversions_status ON conversions (status);

-- =============================================
-- USAGE  (daily per-tool usage counters)
-- user_id nullable for guest tracking by IP (future)
-- =============================================
CREATE TABLE usage (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    tool    VARCHAR(50) NOT NULL,
    date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    count   INT         NOT NULL DEFAULT 1,
    UNIQUE (user_id, tool, date)
);

CREATE INDEX idx_usage_user_date ON usage (user_id, date);
