-- V3 - Expande a tabela de contratos com campos da SPEC + vínculo com licitação

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS bidding_id UUID REFERENCES biddings(id),
    ADD COLUMN IF NOT EXISTS preposto_nome VARCHAR(150),
    ADD COLUMN IF NOT EXISTS gestor_orgao VARCHAR(150),
    ADD COLUMN IF NOT EXISTS fiscal_tecnico VARCHAR(150),
    ADD COLUMN IF NOT EXISTS fiscal_administrativo VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_contracts_bidding ON contracts(bidding_id);
