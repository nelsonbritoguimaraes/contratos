-- V35: Contabilidade completa — períodos, centro de custo, referencial SPED, provisões

CREATE TABLE IF NOT EXISTS accounting_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    competencia DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    fechado_em TIMESTAMPTZ,
    fechado_por VARCHAR(150),
    observacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_accounting_period UNIQUE (tenant_id, competencia)
);

ALTER TABLE contas_contabeis
    ADD COLUMN IF NOT EXISTS codigo_referencial VARCHAR(30);

ALTER TABLE lancamentos_contabeis
    ADD COLUMN IF NOT EXISTS cost_center_id UUID REFERENCES cost_centers(id);

CREATE INDEX IF NOT EXISTS idx_lancamentos_cost_center ON lancamentos_contabeis (cost_center_id);
CREATE INDEX IF NOT EXISTS idx_lancamentos_origem ON lancamentos_contabeis (tenant_id, origem_tipo, origem_id);

-- Provisões a Pagar (faltava no seed V27)
INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa, codigo_referencial)
SELECT id, '2.1.05', 'Provisões Trabalhistas a Pagar', 'PASSIVO', 'CREDORA', 3, true, true, '2.01.05.01.01'
FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM contas_contabeis c WHERE c.tenant_id = t.id AND c.codigo = '2.1.05');

-- Mapeamento referencial básico (Plano Referencial RFB — Lucro Presumido)
UPDATE contas_contabeis SET codigo_referencial = '1.01.01.01.01' WHERE codigo = '1.1.01' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '1.01.03.01.01' WHERE codigo = '1.1.02' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '2.01.01.01.01' WHERE codigo = '2.1.01' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '2.01.02.01.01' WHERE codigo = '2.1.02' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '2.01.03.01.01' WHERE codigo = '2.1.03' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '2.01.04.01.01' WHERE codigo = '2.1.04' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '3.01.01.01.01' WHERE codigo = '3.1.01' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '3.01.02.01.01' WHERE codigo = '3.1.02' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '4.01.01.01.01' WHERE codigo = '4.1.01' AND codigo_referencial IS NULL;
UPDATE contas_contabeis SET codigo_referencial = '3.02.01.01.01' WHERE codigo = '3.2.01' AND codigo_referencial IS NULL;

CREATE TABLE IF NOT EXISTS accounting_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    descricao VARCHAR(200),
    origem_tipo VARCHAR(50) NOT NULL,
    conta_debito_codigo VARCHAR(30) NOT NULL,
    conta_credito_codigo VARCHAR(30) NOT NULL,
    historico_padrao VARCHAR(255),
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'FOLHA_LIQUIDO', 'Folha — líquido a pagar', 'PAYSLIP', '3.1.01', '2.1.01', 'Folha de pagamento — líquido'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.codigo = 'FOLHA_LIQUIDO');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'FOLHA_INSS', 'Folha — INSS', 'PAYSLIP', '3.1.02', '2.1.03', 'Encargos INSS'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.codigo = 'FOLHA_INSS');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'FOLHA_FGTS', 'Folha — FGTS', 'PAYSLIP', '3.1.02', '2.1.02', 'Encargos FGTS'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.codigo = 'FOLHA_FGTS');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'GLOSA', 'Glosa contratual', 'GLOSA', '3.2.01', '1.1.02', 'Glosa aplicada — redução receita'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.codigo = 'GLOSA');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'MEDICAO', 'Faturamento medição', 'MEASUREMENT', '1.1.02', '4.1.01', 'Receita de serviços — medição'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.codigo = 'MEDICAO');
