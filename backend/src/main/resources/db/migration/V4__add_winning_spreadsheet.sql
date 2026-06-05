-- V4 - Tabela de Planilha Vencedora (WinningSpreadsheet)

CREATE TABLE IF NOT EXISTS winning_spreadsheets (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bidding_id          UUID REFERENCES biddings(id),
    contract_id         UUID REFERENCES contracts(id),
    versao              INTEGER NOT NULL DEFAULT 1,
    arquivo_nome        VARCHAR(255),
    arquivo_url         TEXT,
    memoria_calculo     TEXT,
    is_vencedora        BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_winning_spreadsheets_bidding ON winning_spreadsheets(bidding_id);
CREATE INDEX IF NOT EXISTS idx_winning_spreadsheets_contract ON winning_spreadsheets(contract_id);
