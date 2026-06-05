-- V11 - Tabelas de Glosas e IMR (SPEC seções 17, 18 e 25.5)

CREATE TABLE IF NOT EXISTS glosa_rules (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id     UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    rule_type       VARCHAR(50) NOT NULL,
    description     TEXT,
    factor          NUMERIC(5,2) DEFAULT 1.0,
    tolerance_minutes INTEGER,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    priority        INTEGER NOT NULL DEFAULT 10,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS imr_indicators (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id     UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    weight          NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    measurement_method VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS imr_ranges (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    indicator_id        UUID NOT NULL REFERENCES imr_indicators(id) ON DELETE CASCADE,
    min_value           NUMERIC(5,2) NOT NULL,
    max_value           NUMERIC(5,2) NOT NULL,
    deduction_percent   NUMERIC(5,2) NOT NULL,
    priority            INTEGER DEFAULT 10
);

CREATE TABLE IF NOT EXISTS glosas (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    measurement_period  DATE NOT NULL,
    glosa_type          VARCHAR(50) NOT NULL,
    description         TEXT,
    base_value          NUMERIC(14,2),
    glosa_amount        NUMERIC(14,2) NOT NULL,
    status              VARCHAR(30) DEFAULT 'APURADA',
    evidence_url        TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_glosa_rules_contract ON glosa_rules(contract_id);
CREATE INDEX IF NOT EXISTS idx_imr_indicators_contract ON imr_indicators(contract_id);
CREATE INDEX IF NOT EXISTS idx_glosas_contract_period ON glosas(contract_id, measurement_period);

COMMENT ON TABLE glosa_rules IS 'Regras configuráveis de glosa por contrato - SPEC §17';
COMMENT ON TABLE imr_indicators IS 'Indicadores de IMR - SPEC §18';
COMMENT ON TABLE glosas IS 'Glosas apuradas - SPEC §17';
