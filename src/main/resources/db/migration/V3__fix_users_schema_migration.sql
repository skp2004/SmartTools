-- V3__fix_users_schema_migration.sql
-- Safe idempotent fix: migrate old V1 users schema → V2 SmartTools schema.
-- This handles the case where Flyway ran V1 (old invoice schema) but V2 either
-- did not run or ran partially, leaving the users table with incorrect columns.

-- ─── USERS TABLE FIXES ────────────────────────────────────────────────────

-- 1. Rename auth_provider → provider (V1 used auth_provider, V2 uses provider)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'auth_provider'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'provider'
    ) THEN
        ALTER TABLE users RENAME COLUMN auth_provider TO provider;
    END IF;
END;
$$;

-- 2. Rename google_id → provider_id (V1 used google_id, V2 uses provider_id)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'google_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'provider_id'
    ) THEN
        ALTER TABLE users RENAME COLUMN google_id TO provider_id;
    END IF;
END;
$$;

-- 3. Add provider column if it doesn't exist at all
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'provider'
    ) THEN
        ALTER TABLE users ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL'
            CHECK (provider IN ('LOCAL', 'GOOGLE'));
    END IF;
END;
$$;

-- 4. Add provider_id column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'provider_id'
    ) THEN
        ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);
    END IF;
END;
$$;

-- 5. Add role column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'role'
    ) THEN
        ALTER TABLE users ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER'
            CHECK (role IN ('USER', 'ADMIN'));
    END IF;
END;
$$;

-- 6. Add enabled column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'enabled'
    ) THEN
        ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
END;
$$;

-- 7. Add email_verified column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'email_verified'
    ) THEN
        ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END;
$$;

-- 8. Add picture_url column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'picture_url'
    ) THEN
        ALTER TABLE users ADD COLUMN picture_url VARCHAR(512);
    END IF;
END;
$$;

-- 9. Add updated_at column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE users ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;
END;
$$;

-- ─── SUBSCRIPTIONS TABLE FIXES ────────────────────────────────────────────

-- Recreate subscriptions table if it uses old schema (V1 had razorpay columns)
-- Safely add columns if they're missing from subscriptions
DO $$
BEGIN
    -- Check if old razorpay_subscription_id column exists (V1 schema indicator)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'subscriptions' AND column_name = 'razorpay_subscription_id'
    ) THEN
        -- Drop and recreate with correct V2 schema
        DROP TABLE IF EXISTS subscriptions CASCADE;
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
    END IF;
END;
$$;

-- ─── CONVERSIONS TABLE ────────────────────────────────────────────────────

-- Create conversions table if it doesn't exist
CREATE TABLE IF NOT EXISTS conversions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    tool                VARCHAR(50)  NOT NULL,
    status              VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED')),
    input_filename      VARCHAR(500),
    output_filename     VARCHAR(500),
    file_size_bytes     BIGINT,
    processing_time_ms  BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conversions_user   ON conversions (user_id);
CREATE INDEX IF NOT EXISTS idx_conversions_tool   ON conversions (tool);
CREATE INDEX IF NOT EXISTS idx_conversions_status ON conversions (status);

-- ─── USAGE TABLE ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS usage (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    tool    VARCHAR(50) NOT NULL,
    date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    count   INT         NOT NULL DEFAULT 1,
    UNIQUE (user_id, tool, date)
);

CREATE INDEX IF NOT EXISTS idx_usage_user_date ON usage (user_id, date);

-- ─── INDEXES ──────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_users_email    ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_provider ON users (provider, provider_id) WHERE provider_id IS NOT NULL;
