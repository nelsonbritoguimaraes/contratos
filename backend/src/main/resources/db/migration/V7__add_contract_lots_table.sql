-- V7 - Tabela de Lotes de Contrato (ContractLot)
-- Representa os lotes efetivamente contratados a partir de uma licitação.
-- Alinhado com a entidade JPA ContractLot e SPEC v1.0 seções 5-6.

CREATE TABLE IF NOT EXISTS contract_lots (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id              UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id            UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    bidding_lot_id         UUID REFERENCES bidding_lots(id),
    numero_lote            VARCHAR(50),
    descricao              TEXT,
    quantitativo_postos    INTEGER NOT NULL DEFAULT 0,
    valor_mensal           NUMERIC(14,2),
    valor_global           NUMERIC(16,2),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_contract_lots_tenant ON contract_lots(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contract_lots_contract ON contract_lots(contract_id);
CREATE INDEX IF NOT EXISTS idx_contract_lots_bidding_lot ON contract_lots(bidding_lot_id);

COMMENT ON TABLE contract_lots IS 'Lotes efetivamente contratados (provenientes de bidding_lots). Fase 1 do fluxo Licitação → Contrato.';
