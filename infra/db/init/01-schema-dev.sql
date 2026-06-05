-- ContractOps AI — Schema DEMO para Scaffold v0.0.1
-- Este schema é SOMENTE para o ambiente de desenvolvimento local e demonstração.
-- O schema real será gerado por Flyway/Liquibase no backend Kotlin (ver seção 25 da SPEC).
--
-- Todas as tabelas já incluem tenant_id preparando para multi-tenancy + RLS futuro.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TENANTS E ORGANIZAÇÃO (base do multi-tenant)
-- ============================================

CREATE TABLE IF NOT EXISTS tenants (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) UNIQUE NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS companies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    cnpj            VARCHAR(18) NOT NULL,
    razao_social    VARCHAR(255) NOT NULL,
    nome_fantasia   VARCHAR(255),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    UNIQUE (tenant_id, cnpj)
);

CREATE TABLE IF NOT EXISTS branches (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    city            VARCHAR(100),
    state           VARCHAR(2),
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- CONTRATOS (núcleo do produto)
-- ============================================

CREATE TABLE IF NOT EXISTS contracts (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id              UUID NOT NULL REFERENCES companies(id),
    branch_id               UUID REFERENCES branches(id),
    numero                  VARCHAR(50) NOT NULL,
    orgao                   VARCHAR(255) NOT NULL,
    cnpj_orgao              VARCHAR(18),
    objeto                  TEXT,
    vigencia_inicio         DATE,
    vigencia_fim            DATE,
    valor_mensal            NUMERIC(14,2),
    valor_global            NUMERIC(16,2),
    status                  VARCHAR(30) DEFAULT 'ATIVO', -- ATIVO, SUSPENSO, ENCERRADO, EM_IMPLANTACAO
    qtd_postos_contratados  INTEGER DEFAULT 0,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now(),
    UNIQUE (tenant_id, numero)
);

-- ============================================
-- POSTOS DE SERVIÇO (ServicePost)
-- ============================================

CREATE TABLE IF NOT EXISTS service_posts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    codigo              VARCHAR(50),
    nome                VARCHAR(150) NOT NULL,
    funcao              VARCHAR(100),
    cbo                 VARCHAR(10),
    escala              VARCHAR(20),           -- 12x36, 5x2, 6x1, PLANTAO
    jornada_horas       INTEGER,
    valor_mensal        NUMERIC(12,2),
    valor_diario        NUMERIC(10,2),
    status              VARCHAR(30) DEFAULT 'ATIVO',  -- ATIVO, VAGO, SUSPENSO, AGUARDANDO_IMPLANTACAO
    titular_nome        VARCHAR(150),
    created_at          TIMESTAMPTZ DEFAULT now()
);

-- Índices úteis para os grids da SPEC
CREATE INDEX IF NOT EXISTS idx_contracts_tenant ON contracts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contracts_orgao ON contracts(orgao);
CREATE INDEX IF NOT EXISTS idx_posts_contract ON service_posts(contract_id);
CREATE INDEX IF NOT EXISTS idx_posts_tenant ON service_posts(tenant_id);

-- Comentário: futuramente adicionar tabelas completas da seção 25 da SPEC
-- (Bidding, WinningSpreadsheet, Employee, Measurement, Glosa, PayrollPeriod, etc.)
