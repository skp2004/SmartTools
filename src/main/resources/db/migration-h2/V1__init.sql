-- H2-compatible version of the schema for local testing without Docker
-- This is NOT used in dev/prod — only for build verification

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    name            VARCHAR(255) NOT NULL,
    auth_provider   VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    google_id       VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS companies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id   BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    logo_url        VARCHAR(512),
    gstin           VARCHAR(15),
    address         CLOB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clients (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id      BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    address         CLOB,
    gstin           VARCHAR(15),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id      BIGINT         NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    client_id       BIGINT         NOT NULL REFERENCES clients (id),
    invoice_number  VARCHAR(50)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    issue_date      DATE           NOT NULL,
    due_date        DATE           NOT NULL,
    subtotal        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    tax_total       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total           NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes           CLOB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_number_per_company UNIQUE (company_id, invoice_number)
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT         NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    description     VARCHAR(500)   NOT NULL,
    quantity        NUMERIC(10, 2) NOT NULL,
    rate            NUMERIC(15, 2) NOT NULL,
    tax_percent     NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    line_total      NUMERIC(15, 2) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    razorpay_subscription_id VARCHAR(255),
    plan                     VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status                   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_period_end       TIMESTAMP WITH TIME ZONE,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
