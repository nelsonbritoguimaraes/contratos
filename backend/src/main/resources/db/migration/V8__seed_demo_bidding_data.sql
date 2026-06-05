-- ============================================================
-- DEV SEED ONLY — DO NOT RUN IN PRODUCTION
-- ============================================================
-- V8 - Seed de dados demonstrativos para Licitações, Lotes e Planilhas Vencedoras
-- Executado após V1-V7 pelo Flyway.
-- Liga com os contratos do seed do infra (tenant fixo 11111111-...).

-- ============================================
-- 3 LICITAÇÕES (biddings)
-- ============================================
INSERT INTO biddings (id, tenant_id, processo_numero, edital_numero, modalidade, portal_origem, orgao, cnpj_orgao, objeto, data_publicacao, data_sessao, data_homologacao, data_adjudicacao, valor_estimado, valor_vencedor, status, fonte_recurso)
VALUES
('b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1', '11111111-1111-1111-1111-111111111111',
 'PE 45/2024', '045/2024', 'Pregão Eletrônico', 'Compras.gov.br',
 'Prefeitura Municipal de Curitiba', '75.357.463/0001-43',
 'Prestação de serviços de vigilância armada e desarmada com dedicação exclusiva de mão de obra',
 '2024-10-15', '2024-11-20', '2024-12-05', '2024-12-10',
 1920000.00, 187450.00 * 24, 'HOMOLOGADA', 'Recursos próprios do município'),

('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', '11111111-1111-1111-1111-111111111111',
 'PE 112/2024', '112/2024', 'Pregão Eletrônico', 'Portal de Compras do Estado do Paraná',
 'Estado do Paraná - Secretaria de Segurança', '76.416.782/0001-59',
 'Vigilância patrimonial em unidades administrativas do interior',
 '2024-06-01', '2024-07-10', '2024-07-25', '2024-07-30',
 980000.00, 94500.00 * 12, 'HOMOLOGADA', 'Tesouro Estadual'),

('b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3', '11111111-1111-1111-1111-111111111111',
 'PE 78/2025', '078/2025', 'Pregão Eletrônico', 'Compras.gov.br',
 'Prefeitura Municipal de Cascavel', '75.357.463/0001-43',
 'Serviços de limpeza e conservação predial com dedicação exclusiva',
 '2025-01-20', '2025-02-18', '2025-02-28', '2025-03-05',
 1750000.00, 67200.00 * 24, 'HOMOLOGADA', 'Recursos próprios do município')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- LOTES DAS LICITAÇÕES (bidding_lots)
-- ============================================
INSERT INTO bidding_lots (id, tenant_id, bidding_id, numero_lote, descricao, quantitativo_postos, valor_mensal, valor_global, prazo_meses)
VALUES
-- Lotes da Licitação 1 (Curitiba)
('l1a1a1a1-l1a1-l1a1-l1a1-l1a1a1a1a1a1', '11111111-1111-1111-1111-111111111111', 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1',
 'Lote 01', 'Vigilância - Sede da Prefeitura e secretarias centrais', 8, 125000.00, 3000000.00, 24),
('l1a1a1a2-l1a1-l1a1-l1a1-l1a1a1a1a1a2', '11111111-1111-1111-1111-111111111111', 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1',
 'Lote 02', 'Vigilância - Unidades de saúde e escolas', 4, 62450.00, 1498800.00, 24),

-- Lotes da Licitação 2 (Paraná)
('l2b2b2b1-l2b2-l2b2-l2b2-l2b2b2b2b2b1', '11111111-1111-1111-1111-111111111111', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2',
 'Lote Único', 'Vigilância patrimonial - 12 unidades administrativas do interior', 8, 94500.00, 1134000.00, 12),

-- Lotes da Licitação 3 (Cascavel)
('l3c3c3c1-l3c3-l3c3-l3c3-l3c3c3c3c3c1', '11111111-1111-1111-1111-111111111111', 'b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3',
 'Lote 01', 'Limpeza - Prédios públicos centrais', 10, 45000.00, 1080000.00, 24),
('l3c3c3c2-l3c3-l3c3-l3c3-l3c3c3c3c3c2', '11111111-1111-1111-1111-111111111111', 'b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3',
 'Lote 02', 'Limpeza - Escolas e UBS', 5, 22200.00, 532800.00, 24)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- PLANILHAS VENCEDORAS (winning_spreadsheets)
-- ============================================
INSERT INTO winning_spreadsheets (id, tenant_id, bidding_id, contract_id, versao, arquivo_nome, is_vencedora, memoria_calculo)
VALUES
('w1w1w1w1-w1w1-w1w1-w1w1-w1w1w1w1w1w1', '11111111-1111-1111-1111-111111111111', 'b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1',
 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 1, 'planilha_vencedora_pe045_2024_v1.xlsx', true,
 'Memória de cálculo: 12 postos vigilante diurno + 4 noturno. Composição: salário + encargos + uniformes + equipamentos.'),

('w2w2w2w2-w2w2-w2w2-w2w2-w2w2w2w2w2w2', '11111111-1111-1111-1111-111111111111', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2',
 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 1, 'planilha_vencedora_pe112_2024_v1.xlsx', true,
 '8 postos de vigilante 12x36. Valor mensal por posto R$ 11.812,50 (salário + 80% encargos + benefícios).'),

('w3w3w3w3-w3w3-w3w3-w3w3-w3w3w3w3w3w3', '11111111-1111-1111-1111-111111111111', 'b3b3b3b3-b3b3-b3b3-b3b3-b3b3b3b3b3b3',
 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 1, 'planilha_vencedora_pe078_2025_v1.xlsx', true,
 '15 postos de auxiliar de limpeza + 1 supervisor. Turnos: 6h diárias.')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- LOTES DE CONTRATO (contract_lots) - vinculados aos contratos existentes
-- ============================================
INSERT INTO contract_lots (id, tenant_id, contract_id, bidding_lot_id, numero_lote, descricao, quantitativo_postos, valor_mensal, valor_global)
VALUES
('cl1cl1c1-cl1c-cl1c-cl1c-cl1cl1cl1c1c1', '11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1',
 'l1a1a1a1-l1a1-l1a1-l1a1-l1a1a1a1a1a1', 'Lote 01', 'Vigilância - Sede da Prefeitura e secretarias centrais (Contrato 45/2024)', 8, 125000.00, 3000000.00),

('cl1cl1c2-cl1c-cl1c-cl1c-cl1cl1cl1c2c2', '11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1',
 'l1a1a1a2-l1a1-l1a1-l1a1-l1a1a1a1a1a2', 'Lote 02', 'Vigilância - Unidades de saúde e escolas (Contrato 45/2024)', 4, 62450.00, 1498800.00),

('cl2cl2c1-cl2c-cl2c-cl2c-cl2cl2cl2c1c1', '11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2',
 'l2b2b2b1-l2b2-l2b2-l2b2-l2b2b2b2b2b1', 'Lote Único', 'Vigilância patrimonial - 12 unidades administrativas do interior', 8, 94500.00, 1134000.00),

('cl3cl3c1-cl3c-cl3c-cl3c-cl3cl3cl3c1c1', '11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
 'l3c3c3c1-l3c3-l3c3-l3c3-l3c3c3c3c3c1', 'Lote 01', 'Limpeza - Prédios públicos centrais (Contrato 78/2025)', 10, 45000.00, 1080000.00)
ON CONFLICT (id) DO NOTHING;

-- Atualiza contagem de postos nos contratos (para consistência com qtd_postos_contratados)
UPDATE contracts SET qtd_postos_contratados = 12 WHERE id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1';
UPDATE contracts SET qtd_postos_contratados = 8  WHERE id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2';
UPDATE contracts SET qtd_postos_contratados = 15 WHERE id = 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3';

COMMENT ON TABLE biddings IS 'Seed demo V8 - dados de licitações para testes das APIs /api/biddings e fluxo completo.';
