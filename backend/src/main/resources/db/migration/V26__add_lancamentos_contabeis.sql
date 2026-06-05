-- V26 - Módulo de Contabilidade: Lançamentos Contábeis
-- Fase 4 da SPEC

CREATE TABLE IF NOT EXISTS lancamentos_contabeis (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    data                DATE NOT NULL,
    conta_debito_id     UUID NOT NULL REFERENCES contas_contabeis(id),
    conta_credito_id    UUID NOT NULL REFERENCES contas_contabeis(id),
    valor               NUMERIC(16,2) NOT NULL,
    historico           TEXT,
    origem_tipo         VARCHAR(50),                    -- MEASUREMENT, PAYSLIP, GLOSA, MANUAL, PROVISAO...
    origem_id           UUID,
    contrato_id         UUID REFERENCES contracts(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_lancamentos_tenant_data ON lancamentos_contabeis(tenant_id, data);
CREATE INDEX IF NOT EXISTS idx_lancamentos_origem ON lancamentos_contabeis(tenant_id, origem_tipo, origem_id);
CREATE INDEX IF NOT EXISTS idx_lancamentos_contrato ON lancamentos_contabeis(tenant_id, contrato_id, data);

COMMENT ON TABLE lancamentos_contabeis IS 'Lançamentos contábeis com rastreabilidade de origem - Fase 4';
COMMENT ON COLUMN lancamentos_contabeis.origem_tipo IS 'MEASUREMENT, PAYSLIP, GLOSA, MANUAL, PROVISAO_RH, etc.';
