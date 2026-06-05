-- V10 - Tabela de Aditivos e Alterações Contratuais (SPEC seção 6.2)

CREATE TABLE IF NOT EXISTS contract_amendments (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id              UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id            UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    amendment_number       VARCHAR(50),
    type                   VARCHAR(50) NOT NULL,           -- PRORROGACAO, ACRESCIMO, SUPRESSAO, REPACTUACAO, REAJUSTE, REEQUILIBRIO
    description            TEXT,
    effective_date         DATE,
    new_end_date           DATE,
    value_change           NUMERIC(16,2),
    new_monthly_value      NUMERIC(14,2),
    status                 VARCHAR(30) DEFAULT 'VIGENTE',
    document_url           TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_contract_amendments_tenant ON contract_amendments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contract_amendments_contract ON contract_amendments(contract_id);

COMMENT ON TABLE contract_amendments IS 'Aditivos, apostilamentos, repactuações e reajustes - SPEC §6.2';
