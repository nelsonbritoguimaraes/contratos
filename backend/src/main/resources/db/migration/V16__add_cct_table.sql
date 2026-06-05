-- V16 - CCT (Convenção Coletiva de Trabalho)
-- SPEC §4.15, §26 — base para regras salariais, benefícios e glosas

CREATE TABLE IF NOT EXISTS ccts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID REFERENCES contracts(id),
    sindicato           VARCHAR(200),
    vigencia_inicio     DATE,
    vigencia_fim        DATE,
    arquivo_nome        VARCHAR(255),
    raw_text            TEXT,
    extracted_data      TEXT,
    status              VARCHAR(30) DEFAULT 'ATIVO',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_ccts_tenant ON ccts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ccts_contract ON ccts(tenant_id, contract_id);
CREATE INDEX IF NOT EXISTS idx_ccts_vigencia ON ccts(vigencia_inicio, vigencia_fim);

COMMENT ON TABLE ccts IS 'Convenções Coletivas de Trabalho vinculadas a contratos ou empresas. Usado para extração de regras salariais e benefícios (SPEC 4.15).';
