-- V31 — Fornecedores, lançamentos manuais, workflows financeiros, Open Finance, match NFS-e

CREATE TABLE fornecedores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    cnpj VARCHAR(20),
    contato VARCHAR(120),
    categoria VARCHAR(100),
    ativo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_fornecedores_tenant_cnpj UNIQUE (tenant_id, cnpj)
);

CREATE INDEX idx_fornecedores_tenant ON fornecedores(tenant_id);
CREATE INDEX idx_fornecedores_ativo ON fornecedores(tenant_id, ativo);

CREATE TABLE lancamentos_financeiros (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    fornecedor_id UUID REFERENCES fornecedores(id),
    descricao TEXT NOT NULL,
    valor NUMERIC(16,2) NOT NULL,
    data_lancamento DATE NOT NULL,
    categoria VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    conta_a_pagar_id UUID,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_lancamentos_fin_tenant ON lancamentos_financeiros(tenant_id);
CREATE INDEX idx_lancamentos_fin_tipo ON lancamentos_financeiros(tenant_id, tipo);

CREATE TABLE finance_workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tipo VARCHAR(50) NOT NULL DEFAULT 'NFS_COBRANCA_CONCILIACAO',
    nota_fiscal_id UUID,
    conta_a_receber_id UUID,
    estado_atual VARCHAR(50) NOT NULL,
    historico_json TEXT,
    erro TEXT,
    concluido BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_finance_workflows_tenant ON finance_workflows(tenant_id, concluido);

CREATE TABLE open_finance_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_bancaria_id UUID,
    payload_json TEXT NOT NULL,
    processado BOOLEAN NOT NULL DEFAULT false,
    itens_importados INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE extratos_bancarios_itens
    ADD COLUMN IF NOT EXISTS match_confidence NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS nota_fiscal_id UUID,
    ADD COLUMN IF NOT EXISTS match_metodo VARCHAR(40);

ALTER TABLE notas_fiscais_servico
    ADD COLUMN IF NOT EXISTS tomador_email VARCHAR(200),
    ADD COLUMN IF NOT EXISTS danfse_html TEXT,
    ADD COLUMN IF NOT EXISTS email_orgao_enviado_em TIMESTAMPTZ;

-- Seed demo (primeiro tenant)
INSERT INTO fornecedores (tenant_id, razao_social, cnpj, contato, categoria)
SELECT t.id, 'Uniformes Profissionais Ltda', '12345678000190', '(41) 99999-1234', 'Uniformes e EPIs'
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedores f WHERE f.tenant_id = t.id AND f.cnpj = '12345678000190'
)
LIMIT 1;

INSERT INTO fornecedores (tenant_id, razao_social, cnpj, contato, categoria)
SELECT t.id, 'Limpeza Total S/A', '98765432000110', '(41) 3333-5678', 'Produtos de Limpeza'
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1 FROM fornecedores f WHERE f.tenant_id = t.id AND f.cnpj = '98765432000110'
)
LIMIT 1;
