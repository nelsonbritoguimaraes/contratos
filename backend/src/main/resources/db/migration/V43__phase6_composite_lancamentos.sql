-- V43: Contabilidade Phase 6 — lançamentos compostos + filial

ALTER TABLE lancamentos_contabeis
    ADD COLUMN IF NOT EXISTS branch_id UUID,
    ADD COLUMN IF NOT EXISTS composto BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS lancamento_contabil_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lancamento_id UUID NOT NULL REFERENCES lancamentos_contabeis(id) ON DELETE CASCADE,
    linha_ordem INT NOT NULL DEFAULT 1,
    conta_id UUID NOT NULL REFERENCES contas_contabeis(id),
    natureza_linha VARCHAR(1) NOT NULL,
    valor NUMERIC(16,2) NOT NULL,
    historico_linha VARCHAR(255),
    cost_center_id UUID REFERENCES cost_centers(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_lancamento_line_natureza CHECK (natureza_linha IN ('D', 'C'))
);

CREATE INDEX IF NOT EXISTS idx_lancamento_lines_header ON lancamento_contabil_lines (lancamento_id, linha_ordem);
CREATE INDEX IF NOT EXISTS idx_lancamentos_branch ON lancamentos_contabeis (tenant_id, branch_id);

COMMENT ON TABLE lancamento_contabil_lines IS 'Partidas múltiplas de lançamento contábil composto';

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa, codigo_referencial)
SELECT id, '2.1.06', 'Retenções Tributárias a Recolher', 'PASSIVO', 'CREDORA', 3, true, true, '2.01.06.01.01'
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis c WHERE c.tenant_id = t.id AND c.codigo = '2.1.06');
