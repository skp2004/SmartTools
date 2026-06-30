-- V1__init.sql
-- Multi-tenant SaaS Invoice Generator schema
-- Every business-data table is scoped by company_id from the start.
-- Enums stored as VARCHAR with CHECK constraints (avoids Hibernate/Postgres native enum headaches).

-- =============================================
-- USERS
-- =============================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),           -- nullable for Google-only users
    name            VARCHAR(255) NOT NULL,
    auth_provider   VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
                    CHECK (auth_provider IN ('LOCAL', 'GOOGLE')),
    google_id       VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- =============================================
-- COMPANIES  (one per user — the user's "tenant")
-- =============================================
CREATE TABLE companies (
    id              BIGSERIAL PRIMARY KEY,
    owner_user_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    logo_url        VARCHAR(512),
    gstin           VARCHAR(15),
    address         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_companies_owner ON companies (owner_user_id);

-- =============================================
-- CLIENTS  (scoped to a company)
-- =============================================
CREATE TABLE clients (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    address         TEXT,
    gstin           VARCHAR(15),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_clients_company ON clients (company_id);

-- =============================================
-- INVOICES  (scoped to a company)
-- =============================================
CREATE TABLE invoices (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT         NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    client_id       BIGINT         NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    invoice_number  VARCHAR(50)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'OVERDUE')),
    issue_date      DATE           NOT NULL,
    due_date        DATE           NOT NULL,
    subtotal        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tax_total       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- Invoice numbers must be unique within a company
    CONSTRAINT uq_invoice_number_per_company UNIQUE (company_id, invoice_number)
);

CREATE INDEX idx_invoices_company ON invoices (company_id);
CREATE INDEX idx_invoices_client ON invoices (client_id);
CREATE INDEX idx_invoices_status ON invoices (company_id, status);

-- =============================================
-- INVOICE ITEMS  (children of invoices)
-- =============================================
CREATE TABLE invoice_items (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT         NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    description     VARCHAR(500)   NOT NULL,
    quantity        NUMERIC(10, 2) NOT NULL,
    rate            NUMERIC(15, 2) NOT NULL,
    tax_percent     NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    line_total      NUMERIC(15, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items (invoice_id);

-- =============================================
-- SUBSCRIPTIONS  (SaaS billing, one per user)
-- =============================================
CREATE TABLE subscriptions (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    razorpay_subscription_id VARCHAR(255),
    plan                     VARCHAR(20) NOT NULL DEFAULT 'FREE'
                             CHECK (plan IN ('FREE', 'STARTER', 'PRO')),
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELLED', 'TRIALING')),
    current_period_end       TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_user ON subscriptions (user_id);
