-- V37: Financeiro completo — cobrança, auditoria, CC/filial, perfil tributário

CREATE TABLE IF NOT EXISTS cobrancas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_a_receber_id UUID NOT NULL REFERENCES contas_a_receber(id),
    tipo VARCHAR(20) NOT NULL,
    codigo_pix TEXT,
    qr_code_payload TEXT,
    linha_digitavel VARCHAR(120),
    nosso_numero VARCHAR(40),
    status VARCHAR(30) NOT NULL DEFAULT 'EMITIDA',
    valor NUMERIC(16,2) NOT NULL,
    vencimento DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS financial_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entidade_tipo VARCHAR(50) NOT NULL,
    entidade_id UUID,
    acao VARCHAR(50) NOT NULL,
    usuario VARCHAR(150),
    detalhe TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_financial_audit_tenant ON financial_audit_log (tenant_id, created_at DESC);

ALTER TABLE contas_a_receber
    ADD COLUMN IF NOT EXISTS cost_center_id UUID REFERENCES cost_centers(id),
    ADD COLUMN IF NOT EXISTS branch_id UUID,
    ADD COLUMN IF NOT EXISTS glosa_provisao NUMERIC(16,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tomador_cnpj VARCHAR(18);

ALTER TABLE contas_a_pagar
    ADD COLUMN IF NOT EXISTS cost_center_id UUID REFERENCES cost_centers(id),
    ADD COLUMN IF NOT EXISTS branch_id UUID;

ALTER TABLE notas_fiscais_servico
    ADD COLUMN IF NOT EXISTS reinf_protocolo VARCHAR(100);

CREATE TABLE IF NOT EXISTS tenant_fiscal_profile (
    tenant_id UUID PRIMARY KEY,
    desoneracao_folha BOOLEAN NOT NULL DEFAULT FALSE,
    aliquota_inss_retencao NUMERIC(5,4) NOT NULL DEFAULT 0.1100,
    simples_nacional BOOLEAN NOT NULL DEFAULT FALSE,
    municipio_ibge_padrao VARCHAR(10) DEFAULT '3550308',
    cnpj_prestador VARCHAR(18),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO tenant_fiscal_profile (tenant_id, desoneracao_folha, aliquota_inss_retencao)
SELECT id, FALSE, 0.1100 FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_fiscal_profile p WHERE p.tenant_id = t.id);
