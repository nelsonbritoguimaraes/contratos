-- V22 - Seed de Rubricas Padrão CLT (MVP)
-- Rubricas comuns para contratos de mão de obra exclusiva

INSERT INTO payroll_rubrics (tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order)
SELECT 
    t.id,
    'SALARIO_BASE',
    'Salário Base',
    'PROVENTO',
    'FIXED',
    NULL,
    NULL,
    NULL,
    true, true, true,
    true,
    10
FROM tenants t
ON CONFLICT DO NOTHING;

INSERT INTO payroll_rubrics (tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order)
SELECT 
    t.id,
    'ADICIONAL_INSALUBRIDADE',
    'Adicional de Insalubridade (20%)',
    'PROVENTO',
    'PERCENTAGE_OF_BASE',
    NULL,
    20.00,
    'SALARIO_BASE',
    true, true, true,
    true,
    20
FROM tenants t
ON CONFLICT DO NOTHING;

INSERT INTO payroll_rubrics (tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order)
SELECT 
    t.id,
    'ADICIONAL_PERICULOSIDADE',
    'Adicional de Periculosidade (30%)',
    'PROVENTO',
    'PERCENTAGE_OF_BASE',
    NULL,
    30.00,
    'SALARIO_BASE',
    true, true, true,
    true,
    25
FROM tenants t
ON CONFLICT DO NOTHING;

INSERT INTO payroll_rubrics (tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order)
SELECT 
    t.id,
    'INSS',
    'Contribuição Previdenciária (INSS)',
    'DESCONTO',
    'PERCENTAGE_OF_BASE',
    NULL,
    7.50,   -- Faixa inicial simplificada
    'SALARIO_BASE',
    false, false, false,
    true,
    80
FROM tenants t
ON CONFLICT DO NOTHING;

INSERT INTO payroll_rubrics (tenant_id, code, description, type, calculation_type, fixed_value, percentage, reference, incides_inss, incides_fgts, incides_irrf, is_active, display_order)
SELECT 
    t.id,
    'VALE_TRANSPORTE',
    'Vale Transporte (6% do salário)',
    'DESCONTO',
    'PERCENTAGE_OF_BASE',
    NULL,
    6.00,
    'SALARIO_BASE',
    false, false, false,
    true,
    85
FROM tenants t
ON CONFLICT DO NOTHING;

-- Nota: FGTS (8%) é provisionado, não descontado do colaborador na maioria dos casos.
