-- V9 - Adiciona tabela de Centros de Custo (CostCenter)
-- Empresas e Filiais já existem no V1. SPEC seções 4 e 25.1

CREATE TABLE IF NOT EXISTS cost_centers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    branch_id       UUID REFERENCES branches(id),
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(50),
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_cost_centers_tenant ON cost_centers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cost_centers_company ON cost_centers(company_id);
CREATE INDEX IF NOT EXISTS idx_cost_centers_branch ON cost_centers(branch_id);

COMMENT ON TABLE cost_centers IS 'Centros de custo - SPEC seção 4 e 25.1. Usado para rateio de custos por contrato/filial.';
