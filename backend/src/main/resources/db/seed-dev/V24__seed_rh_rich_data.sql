-- ============================================================
-- DEV SEED ONLY — DO NOT RUN IN PRODUCTION
-- ============================================================
-- V24 - Seed rico para demonstração do módulo RH/Folha

-- Exemplo de holerite aprovado (assumindo employee e contrato existentes)
INSERT INTO payslips (tenant_id, employee_id, contract_id, competence, base_salary, total_earnings, total_deductions, net_amount, status, notes)
SELECT 
    e.tenant_id,
    e.id,
    (SELECT id FROM contracts WHERE tenant_id = e.tenant_id LIMIT 1),
    '2026-05-01',
    5200.00,
    5980.00,
    1023.50,
    4956.50,
    'APPROVED',
    'Holerite de exemplo - Maio/2026 (seed rico)'
FROM employees e
LIMIT 1;

-- Exemplo de evento eSocial já "enviado"
INSERT INTO esocial_events (tenant_id, employee_id, event_type, competence, payload, status, generated_at, receipt_number)
SELECT 
    e.tenant_id,
    e.id,
    'S2200',
    '2026-05-01',
    '{"evento":"S-2200","cpf":"' || e.cpf || '","nome":"' || e.full_name || '","dataAdmissao":"' || COALESCE(e.admission_date::text, '2025-01-15') || '"}',
    'SENT',
    NOW(),
    'REC-1748' || FLOOR(RANDOM()*10000)
FROM employees e
LIMIT 1;
