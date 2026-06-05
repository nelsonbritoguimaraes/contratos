-- V2 - Adiciona tabelas de Licitação (Bidding) conforme SPEC v1.0

CREATE TABLE IF NOT EXISTS biddings (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    processo_numero     VARCHAR(100),
    edital_numero       VARCHAR(100),
    modalidade          VARCHAR(100),
    portal_origem       VARCHAR(100),
    orgao               VARCHAR(255) NOT NULL,
    cnpj_orgao          VARCHAR(18),
    objeto              TEXT,
    data_publicacao     DATE,
    data_sessao         DATE,
    data_homologacao    DATE,
    data_adjudicacao    DATE,
    valor_estimado      NUMERIC(16,2),
    valor_vencedor      NUMERIC(16,2),
    status              VARCHAR(50) DEFAULT 'HOMOLOGADA',
    fonte_recurso       VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS bidding_lots (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id              UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id             UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    numero_lote            VARCHAR(50),
    descricao              TEXT,
    quantitativo_postos    INTEGER DEFAULT 0,
    valor_mensal           NUMERIC(14,2),
    valor_anual            NUMERIC(16,2),
    valor_global           NUMERIC(16,2),
    prazo_meses            INTEGER,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_biddings_tenant ON biddings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bidding_lots_bidding ON bidding_lots(bidding_id);
