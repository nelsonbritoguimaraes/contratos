-- ============================================================
-- DEV SEED ONLY — DO NOT RUN IN PRODUCTION
-- ============================================================
-- V23 - Dados de exemplo para módulo RH/Folha (apenas para desenvolvimento/demo)

-- Exemplo de Eventos de DP (assumindo que existem employees com ids fixos no seed anterior)
-- Estes inserts são condicionais e só devem ser usados em ambiente de desenvolvimento.

-- Exemplo de alteração salarial
INSERT INTO employee_events (tenant_id, employee_id, event_type, event_date, description, previous_value, new_value, reason)
SELECT 
    e.tenant_id,
    e.id,
    'SALARY_CHANGE',
    '2026-03-01',
    'Reajuste por promoção',
    4500.00,
    5200.00,
    'Promoção para supervisor de posto'
FROM employees e
WHERE e.cpf = '12345678900'   -- Ajuste conforme seu seed
LIMIT 1;

-- Exemplo de admissão
INSERT INTO employee_events (tenant_id, employee_id, event_type, event_date, description, new_value, reason)
SELECT 
    e.tenant_id,
    e.id,
    'ADMISSION',
    e.admission_date,
    'Admissão no contrato',
    e.salary_base,
    'Admissão inicial'
FROM employees e
LIMIT 1;

-- Nota: Em produção real, esses eventos seriam criados via API.
