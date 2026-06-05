-- V25 - Módulo de Contabilidade: Plano de Contas
-- Fase 4 da SPEC

CREATE TABLE IF NOT EXISTS contas_contabeis (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    codigo              VARCHAR(30) NOT NULL,
    descricao           VARCHAR(200) NOT NULL,
    tipo                VARCHAR(20) NOT NULL,           -- ATIVO, PASSIVO, PATRIMONIO_LIQUIDO, RECEITA, DESPESA
    natureza            VARCHAR(10) NOT NULL,           -- DEVEDORA, CREDORA
    conta_mae_id        UUID REFERENCES contas_contabeis(id),
    nivel               INTEGER NOT NULL DEFAULT 1,
    aceita_lancamento   BOOLEAN NOT NULL DEFAULT TRUE,
    ativa               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_contas_contabeis_tenant_codigo ON contas_contabeis(tenant_id, codigo);
CREATE INDEX IF NOT EXISTS idx_contas_contabeis_tenant_ativa ON contas_contabeis(tenant_id, ativa);

COMMENT ON TABLE contas_contabeis IS 'Plano de Contas por tenant - Fase 4 Contabilidade';
