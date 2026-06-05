-- V38: Open Finance — consentimentos OAuth (fluxo de vinculação de conta)

CREATE TABLE IF NOT EXISTS open_finance_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_bancaria_id UUID,
    institution_id VARCHAR(100),
    institution_name VARCHAR(200),
    consent_id VARCHAR(200),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    authorization_url TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_of_consent_tenant ON open_finance_consents (tenant_id, status);
