-- V17 - Medições / Faturamento básico
-- SPEC §15, §16 — consolida glosas + cobertura de ponto

CREATE TABLE IF NOT EXISTS measurements (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id             UUID NOT NULL REFERENCES contracts(id),
    period                  DATE NOT NULL,
    base_value              NUMERIC(16,2),
    glosa_total             NUMERIC(16,2) DEFAULT 0,
    coverage_adjustment     NUMERIC(16,2) DEFAULT 0,
    final_amount            NUMERIC(16,2),
    status                  VARCHAR(30) DEFAULT 'DRAFT',
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_measurements_tenant_period ON measurements(tenant_id, period);
CREATE INDEX IF NOT EXISTS idx_measurements_contract ON measurements(tenant_id, contract_id, period);
