-- V5 - Adiciona vínculo entre Contrato e Planilha Vencedora

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS winning_spreadsheet_id UUID REFERENCES winning_spreadsheets(id);

CREATE INDEX IF NOT EXISTS idx_contracts_winning_spreadsheet ON contracts(winning_spreadsheet_id);
