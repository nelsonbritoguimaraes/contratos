-- V33 — Módulo de licitações completo (P0–P2): postos, itens, propostas, prazos, documentos, impugnações, atas

ALTER TABLE biddings ADD COLUMN IF NOT EXISTS edital_url VARCHAR(500);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS vencedor_empresa VARCHAR(255);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS unidade_compradora VARCHAR(255);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS regime_legal VARCHAR(80) DEFAULT 'LEI_14133';
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS equipe_responsavel VARCHAR(255);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS garantia_proposta NUMERIC(16,2);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS riscos_identificados TEXT;
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS links_externos TEXT;
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS pncp_id VARCHAR(120);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS numero_ata VARCHAR(100);
ALTER TABLE biddings ADD COLUMN IF NOT EXISTS numero_contrato_ref VARCHAR(100);

CREATE TABLE IF NOT EXISTS bidding_postos (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    bidding_lot_id      UUID REFERENCES bidding_lots(id) ON DELETE SET NULL,
    codigo              VARCHAR(50),
    nome                VARCHAR(255) NOT NULL,
    funcao              VARCHAR(150),
    cbo                 VARCHAR(20),
    escala              VARCHAR(100),
    jornada_horas       INTEGER,
    valor_mensal        NUMERIC(14,2),
    local_execucao      VARCHAR(255),
    municipio_execucao  VARCHAR(150),
    quantidade          INTEGER DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bidding_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_lot_id      UUID NOT NULL REFERENCES bidding_lots(id) ON DELETE CASCADE,
    codigo_item         VARCHAR(50),
    descricao           TEXT NOT NULL,
    unidade             VARCHAR(20),
    quantidade          NUMERIC(14,4) DEFAULT 1,
    valor_unitario      NUMERIC(14,4),
    valor_total         NUMERIC(16,2),
    tipo                VARCHAR(30) DEFAULT 'SERVICO',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bidding_proposals (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    versao              INTEGER NOT NULL DEFAULT 1,
    cenario             VARCHAR(30) DEFAULT 'BASE',
    status              VARCHAR(40) DEFAULT 'RASCUNHO',
    valor_proposta      NUMERIC(16,2),
    margem_estimada_pct NUMERIC(8,4),
    custo_total         NUMERIC(16,2),
    observacoes         TEXT,
    tributacao_regime   VARCHAR(40),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bidding_deadlines (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    tipo                VARCHAR(60) NOT NULL,
    descricao           VARCHAR(255),
    data_limite         TIMESTAMPTZ NOT NULL,
    alerta_dias_antes   INTEGER DEFAULT 3,
    concluido           BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bidding_documents (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    tipo                VARCHAR(60) NOT NULL,
    titulo              VARCHAR(255) NOT NULL,
    arquivo_nome        VARCHAR(255),
    arquivo_path        VARCHAR(500),
    mime_type           VARCHAR(100),
    file_size           BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bidding_impugnacoes (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    tipo                VARCHAR(40) NOT NULL,
    protocolo           VARCHAR(100),
    data_protocolo      DATE,
    status              VARCHAR(40) DEFAULT 'PROTOCOLADO',
    argumentos          TEXT,
    resultado           TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bidding_atas (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID NOT NULL REFERENCES biddings(id) ON DELETE CASCADE,
    numero_ata          VARCHAR(100),
    data_sessao         TIMESTAMPTZ,
    resumo              TEXT,
    arquivo_path        VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_bidding_postos_bidding ON bidding_postos(bidding_id);
CREATE INDEX IF NOT EXISTS idx_bidding_items_lot ON bidding_items(bidding_lot_id);
CREATE INDEX IF NOT EXISTS idx_bidding_proposals_bidding ON bidding_proposals(bidding_id);
CREATE INDEX IF NOT EXISTS idx_bidding_deadlines_bidding ON bidding_deadlines(bidding_id);
CREATE INDEX IF NOT EXISTS idx_bidding_documents_bidding ON bidding_documents(bidding_id);
