-- V34: Ponto eletrônico completo — Portaria 671 PTRP (P0–P2)

-- Dedupe antes da constraint única
DELETE FROM attendance_days a
USING attendance_days b
WHERE a.id > b.id
  AND a.tenant_id = b.tenant_id
  AND a.employee_id = b.employee_id
  AND a.date = b.date;

ALTER TABLE attendance_days
    ADD CONSTRAINT uq_attendance_tenant_employee_date UNIQUE (tenant_id, employee_id, date);

ALTER TABLE raw_punches
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 7),
    ADD COLUMN IF NOT EXISTS source_channel VARCHAR(30) DEFAULT 'DEVICE',
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(14);

CREATE INDEX IF NOT EXISTS idx_raw_punches_tenant_matricula ON raw_punches (tenant_id, matricula);
CREATE INDEX IF NOT EXISTS idx_raw_punches_tenant_cpf ON raw_punches (tenant_id, cpf);

CREATE TABLE IF NOT EXISTS punch_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    contract_id UUID,
    post_id UUID,
    date DATE NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    motivo TEXT NOT NULL,
    evidencia_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    solicitado_por VARCHAR(150),
    aprovado_supervisor_por VARCHAR(150),
    aprovado_dp_por VARCHAR(150),
    antes_json TEXT,
    depois_json TEXT,
    impacto_folha DECIMAL(14, 2),
    impacto_glosa DECIMAL(14, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_punch_adj_tenant_emp_date ON punch_adjustments (tenant_id, employee_id, date);

CREATE TABLE IF NOT EXISTS banco_horas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    contract_id UUID,
    competencia DATE NOT NULL,
    saldo_minutos INT NOT NULL DEFAULT 0,
    credito_minutos INT NOT NULL DEFAULT 0,
    debito_minutos INT NOT NULL DEFAULT 0,
    observacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_banco_horas UNIQUE (tenant_id, employee_id, competencia)
);

CREATE TABLE IF NOT EXISTS ponto_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    contract_id UUID,
    competencia DATE NOT NULL,
    codigo_rubrica VARCHAR(50) NOT NULL,
    descricao VARCHAR(255),
    tipo VARCHAR(20) NOT NULL,
    quantidade DECIMAL(10, 2),
    valor_unitario DECIMAL(14, 2),
    valor_total DECIMAL(14, 2),
    origem VARCHAR(30) DEFAULT 'APURACAO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ponto_events_tenant_comp ON ponto_events (tenant_id, employee_id, competencia);

CREATE TABLE IF NOT EXISTS punch_comprovantes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    raw_punch_id UUID,
    employee_id UUID NOT NULL,
    punch_timestamp TIMESTAMPTZ NOT NULL,
    hash_comprovante VARCHAR(128),
    conteudo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comprovante_emp_time ON punch_comprovantes (tenant_id, employee_id, punch_timestamp DESC);

-- Rubricas de ponto (seed para tenant demo)
INSERT INTO payroll_rubrics (id, tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'HORA_EXTRA', 'Horas extras (50%)', 'PROVENTO', 'FORMULA_SIMPLE', NULL, NULL, 'SALARIO_BASE', true, true, true, true, 20, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payroll_rubrics WHERE tenant_id = '11111111-1111-1111-1111-111111111111' AND code = 'HORA_EXTRA');

INSERT INTO payroll_rubrics (id, tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'PONTO_FALTA', 'Desconto por falta (ponto)', 'DESCONTO', 'FORMULA_SIMPLE', NULL, NULL, 'SALARIO_BASE', false, false, false, true, 21, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payroll_rubrics WHERE tenant_id = '11111111-1111-1111-1111-111111111111' AND code = 'PONTO_FALTA');

INSERT INTO payroll_rubrics (id, tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'ATRASO', 'Desconto por atraso', 'DESCONTO', 'FORMULA_SIMPLE', NULL, NULL, 'SALARIO_BASE', false, false, false, true, 22, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payroll_rubrics WHERE tenant_id = '11111111-1111-1111-1111-111111111111' AND code = 'ATRASO');

INSERT INTO payroll_rubrics (id, tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'COBERTURA_PENALTY', 'Penalidade cobertura ponto', 'DESCONTO', 'FORMULA_SIMPLE', NULL, NULL, 'SALARIO_BASE', false, false, false, true, 23, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payroll_rubrics WHERE tenant_id = '11111111-1111-1111-1111-111111111111' AND code = 'COBERTURA_PENALTY');

INSERT INTO payroll_rubrics (id, tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order, created_at, updated_at)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111', 'ADICIONAL_NOTURNO', 'Adicional noturno (ponto)', 'PROVENTO', 'FORMULA_SIMPLE', NULL, NULL, 'SALARIO_BASE', true, true, true, true, 24, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM payroll_rubrics WHERE tenant_id = '11111111-1111-1111-1111-111111111111' AND code = 'ADICIONAL_NOTURNO');
