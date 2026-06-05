-- ContractOps AI — Fase 1 Core Schema
-- Flyway migration V1 (baseado no seed demo do scaffold + SPEC v1.0 seção 25)
--
-- Este é o schema oficial de produção a partir de agora.
-- O schema anterior em infra/db/init/ era apenas demonstrativo.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TENANTS E ORGANIZAÇÃO (multi-tenant foundation)
-- ============================================

CREATE TABLE tenants (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name         VARCHAR(255) NOT NULL,
    slug         VARCHAR(100) UNIQUE NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE companies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    cnpj            VARCHAR(18) NOT NULL,
    razao_social    VARCHAR(255) NOT NULL,
    nome_fantasia   VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, cnpj)
);

CREATE TABLE branches (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    city        VARCHAR(100),
    state       CHAR(2),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================
-- CONTRATOS (núcleo do produto — SPEC seções 6 e 25)
-- ============================================

CREATE TABLE contracts (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id               UUID NOT NULL REFERENCES companies(id),
    branch_id                UUID REFERENCES branches(id),
    numero                   VARCHAR(50) NOT NULL,
    orgao                    VARCHAR(255) NOT NULL,
    cnpj_orgao               VARCHAR(18),
    objeto                   TEXT,
    vigencia_inicio          DATE,
    vigencia_fim             DATE,
    valor_mensal             NUMERIC(14,2),
    valor_global             NUMERIC(16,2),
    status                   VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    qtd_postos_contratados   INTEGER NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    UNIQUE (tenant_id, numero)
);

CREATE INDEX idx_contracts_tenant ON contracts(tenant_id);
CREATE INDEX idx_contracts_orgao ON contracts(orgao);
CREATE INDEX idx_contracts_status ON contracts(status);

-- ============================================
-- POSTOS DE SERVIÇO (ServicePost — SPEC seções 7 e 25)
-- ============================================

CREATE TABLE service_posts (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id        UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id      UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    codigo           VARCHAR(50),
    nome             VARCHAR(150) NOT NULL,
    funcao           VARCHAR(100),
    cbo              VARCHAR(10),
    escala           VARCHAR(20),
    jornada_horas    INTEGER,
    valor_mensal     NUMERIC(12,2),
    valor_diario     NUMERIC(10,2),
    status           VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    titular_nome     VARCHAR(150),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       VARCHAR(100)
);

CREATE INDEX idx_posts_tenant ON service_posts(tenant_id);
CREATE INDEX idx_posts_contract ON service_posts(contract_id);
CREATE INDEX idx_posts_status ON service_posts(status);

-- ============================================
-- TABELAS FUTURAS (comentadas — serão criadas em migrations subsequentes)
-- bidding, winning_spreadsheets, employees, punches, glosas, etc.
-- ============================================

-- Nota: Este schema é intencionalmente minimalista na Fase 1.
-- Vamos evoluir de forma incremental e controlada por Flyway.