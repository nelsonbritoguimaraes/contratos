-- V36: Parametrização rubrica→conta + workflow transmissão SPED

ALTER TABLE accounting_rules
    ADD COLUMN IF NOT EXISTS rubric_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS rubric_type VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uq_accounting_rule_rubric
    ON accounting_rules (tenant_id, origem_tipo, rubric_code)
    WHERE rubric_code IS NOT NULL;

-- Mapeamentos rubrica → contas (folha)
INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, rubric_code, rubric_type, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'RUBRIC_SALARIO', 'Salário base → despesa/líquido', 'PAYSLIP', 'SALARIO_BASE', 'PROVENTO', '3.1.01', '2.1.01', 'Salário base'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.rubric_code = 'SALARIO_BASE');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, rubric_code, rubric_type, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'RUBRIC_INSS', 'INSS → encargos/passivo', 'PAYSLIP', 'INSS', 'DESCONTO', '3.1.02', '2.1.03', 'INSS retido'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.rubric_code = 'INSS');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, rubric_code, rubric_type, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'RUBRIC_IRRF', 'IRRF → despesa/passivo', 'PAYSLIP', 'IRRF', 'DESCONTO', '3.1.01', '2.1.04', 'IRRF retido'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.rubric_code = 'IRRF');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, rubric_code, rubric_type, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'RUBRIC_FGTS', 'FGTS → encargos/passivo', 'PAYSLIP', 'FGTS', 'ENCARGO', '3.1.02', '2.1.02', 'FGTS 8%'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.rubric_code = 'FGTS');

INSERT INTO accounting_rules (tenant_id, codigo, descricao, origem_tipo, rubric_code, rubric_type, conta_debito_codigo, conta_credito_codigo, historico_padrao)
SELECT id, 'RUBRIC_HORA_EXTRA', 'Hora extra → despesa/líquido', 'PAYSLIP', 'HORA_EXTRA', 'PROVENTO', '3.1.01', '2.1.01', 'Horas extras'
FROM tenants t WHERE NOT EXISTS (SELECT 1 FROM accounting_rules r WHERE r.tenant_id = t.id AND r.rubric_code = 'HORA_EXTRA');

CREATE TABLE IF NOT EXISTS sped_transmissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    competencia_inicio DATE,
    competencia_fim DATE,
    ano_calendario INT,
    status VARCHAR(30) NOT NULL DEFAULT 'RASCUNHO',
    arquivo_hash VARCHAR(64),
    total_registros INT,
    erros_validacao JSONB,
    protocolo VARCHAR(100),
    mensagem TEXT,
    aprovado_por VARCHAR(150),
    transmitido_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sped_transmissions_tenant ON sped_transmissions (tenant_id, tipo, status);
