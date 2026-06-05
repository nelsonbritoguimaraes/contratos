-- ============================================================
-- DEV SEED ONLY — DO NOT RUN IN PRODUCTION
-- ============================================================
-- V29 — Seed rico para o Módulo Financeiro (CFO Demo)
-- Cria dados realistas para testes manuais e demonstração do CFO

-- Contas bancárias de exemplo (para cada tenant)
INSERT INTO contas_bancarias (tenant_id, banco_codigo, banco_nome, agencia, conta, tipo, saldo_atual, ativa)
SELECT 
    id, 
    '001', 
    'Banco do Brasil', 
    '1234-5', 
    '98765-4', 
    'CORRENTE', 
    125000.00, 
    true
FROM tenants;

INSERT INTO contas_bancarias (tenant_id, banco_codigo, banco_nome, agencia, conta, tipo, saldo_atual, ativa)
SELECT 
    id, 
    '033', 
    'Santander', 
    '5678-9', 
    '43210-1', 
    'CORRENTE', 
    87500.00, 
    true
FROM tenants;

-- Contas a Receber de exemplo (vinculadas a medições existentes quando possível)
INSERT INTO contas_a_receber (tenant_id, contrato_id, valor_bruto, valor_liquido, vencimento, status, observacoes)
SELECT 
    id,
    (SELECT id FROM contracts LIMIT 1),
    45000.00,
    42750.00,
    CURRENT_DATE + INTERVAL '15 days',
    'ABERTO',
    'Medição Junho/2026 - Vigilância'
FROM tenants;

INSERT INTO contas_a_receber (tenant_id, contrato_id, valor_bruto, valor_liquido, vencimento, status, observacoes)
SELECT 
    id,
    (SELECT id FROM contracts LIMIT 1),
    32000.00,
    30400.00,
    CURRENT_DATE - INTERVAL '5 days',
    'VENCIDO',
    'Medição Maio/2026 - Limpeza (atrasada)'
FROM tenants;

-- Contas a Pagar de exemplo (folha + tributos)
INSERT INTO contas_a_pagar (tenant_id, origem, valor, vencimento, status, observacoes)
SELECT 
    id,
    'PAYSLIP',
    28500.00,
    CURRENT_DATE + INTERVAL '5 days',
    'ABERTO',
    'Folha Junho/2026 - Líquido a pagar'
FROM tenants;

INSERT INTO contas_a_pagar (tenant_id, origem, valor, vencimento, status, observacoes)
SELECT 
    id,
    'RETENCAO_TRIBUTARIA',
    8750.00,
    CURRENT_DATE + INTERVAL '10 days',
    'ABERTO',
    'GPS + DARF - Junho/2026'
FROM tenants;

-- NFS-e de exemplo
INSERT INTO notas_fiscais_servico (tenant_id, numero, serie, data_emissao, tomador_cnpj, valor_servicos, valor_liquido, iss_retido, outras_retencoes, status, xml)
SELECT 
    id,
    'NFS-202606001',
    '1',
    CURRENT_DATE - INTERVAL '10 days',
    '12345678000190',
    45000.00,
    42750.00,
    2250.00,
    0.00,
    'EMITIDA',
    '<xml>Exemplo NFS-e emitida</xml>'
FROM tenants;

-- Retenções de exemplo
INSERT INTO retencoes_tributarias (tenant_id, tipo, aliquota, base_calculo, valor_retido, status)
SELECT 
    id,
    'ISS',
    0.05,
    45000.00,
    2250.00,
    'PENDENTE'
FROM tenants;

-- Previsões de fluxo de caixa (demo)
INSERT INTO previsoes_financeiras (tenant_id, data, tipo, valor, probabilidade, cenario, descricao)
SELECT 
    id,
    CURRENT_DATE + INTERVAL '7 days',
    'RECEBIMENTO_PROJETADO',
    38000.00,
    85,
    'BASE',
    'Recebimento esperado - Contrato Vigilância'
FROM tenants;

INSERT INTO previsoes_financeiras (tenant_id, data, tipo, valor, probabilidade, cenario, descricao)
SELECT 
    id,
    CURRENT_DATE + INTERVAL '20 days',
    'PAGAMENTO_FOLHA',
    29500.00,
    100,
    'BASE',
    'Folha projetada Julho/2026'
FROM tenants;

COMMENT ON TABLE contas_bancarias IS 'Seed demo V29 - Módulo Financeiro CFO';