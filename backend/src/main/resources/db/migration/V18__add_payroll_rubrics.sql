-- V18 - Módulo de RH / Folha: Rubricas de Pagamento
-- SPEC §8 e Fase 3 (Folha e DP)

CREATE TABLE IF NOT EXISTS payroll_rubrics (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code                VARCHAR(50) NOT NULL,
    description         VARCHAR(200) NOT NULL,
    type                VARCHAR(20) NOT NULL,              -- PROVENTO, DESCONTO
    calculation_type    VARCHAR(30) NOT NULL,              -- FIXED, PERCENTAGE_OF_BASE, FORMULA_SIMPLE
    fixed_value         NUMERIC(12,2),
    percentage          NUMERIC(5,2),
    reference           VARCHAR(50),
    incides_inss        BOOLEAN NOT NULL DEFAULT FALSE,
    incides_fgts        BOOLEAN NOT NULL DEFAULT FALSE,
    incides_irrf        BOOLEAN NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    display_order       INTEGER DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_payroll_rubrics_tenant_code ON payroll_rubrics(tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_payroll_rubrics_tenant_active ON payroll_rubrics(tenant_id, is_active);

COMMENT ON TABLE payroll_rubrics IS 'Rubricas de folha de pagamento (proventos e descontos). Núcleo do módulo de RH/Folha - SPEC Fase 3.';
COMMENT ON COLUMN payroll_rubrics.calculation_type IS 'FIXED = valor fixo, PERCENTAGE_OF_BASE = % sobre referência, FORMULA_SIMPLE = futura expansão';
