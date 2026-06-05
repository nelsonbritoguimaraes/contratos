-- V6 - Expande Contrato com mais campos operacionais da SPEC

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS regras_glosa TEXT,
    ADD COLUMN IF NOT EXISTS regras_substituicao TEXT,
    ADD COLUMN IF NOT EXISTS regras_uniforme TEXT,
    ADD COLUMN IF NOT EXISTS regras_equipamentos TEXT,
    ADD COLUMN IF NOT EXISTS regras_faturamento TEXT,
    ADD COLUMN IF NOT EXISTS regras_ponto TEXT,
    ADD COLUMN IF NOT EXISTS regras_medicao TEXT;
