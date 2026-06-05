-- ContractOps AI — Seed DEMO (Scaffold v0.0.1)
-- 1 Tenant + 3 Empresas + 5 Contratos + 20 Postos de Serviço
-- Dados realistas baseados em contratos públicos brasileiros de segurança e limpeza

-- Tenant principal (Grupo empresarial)
INSERT INTO tenants (id, name, slug) VALUES
('11111111-1111-1111-1111-111111111111', 'Grupo Segurança Brasil Participações S.A.', 'grupo-seguranca-brasil');

-- Empresas (CNPJs fictícios mas com formato válido)
INSERT INTO companies (id, tenant_id, cnpj, razao_social, nome_fantasia) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '12.345.678/0001-90', 'Vigilância Alpha Ltda', 'Alpha Vigilância'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', '23.456.789/0001-01', 'Limpeza Profissional Beta EIRELI', 'Beta Limpeza'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', '34.567.890/0001-12', 'Serviços Integrados Gama Ltda', 'Gama Serviços');

-- Filiais
INSERT INTO branches (id, tenant_id, company_id, name, city, state) VALUES
('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Matriz Curitiba', 'Curitiba', 'PR'),
('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Operação Oeste', 'Cascavel', 'PR');

-- ============================================
-- 5 CONTRATOS (núcleo da demonstração)
-- ============================================

INSERT INTO contracts (id, tenant_id, company_id, branch_id, numero, orgao, cnpj_orgao, objeto, vigencia_inicio, vigencia_fim, valor_mensal, valor_global, status, qtd_postos_contratados) VALUES
('c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
 '45/2024', 'Prefeitura Municipal de Curitiba', '75.357.463/0001-43',
 'Prestação de serviços de vigilância armada e desarmada com dedicação exclusiva de mão de obra',
 '2025-01-01', '2026-12-31', 187450.00, 4498800.00, 'ATIVO', 12),

('c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
 '112/2024', 'Estado do Paraná - Secretaria de Segurança', '76.416.782/0001-59',
 'Vigilância patrimonial em unidades administrativas do interior',
 '2024-08-01', '2025-07-31', 94500.00, 1134000.00, 'ATIVO', 8),

('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2',
 '78/2025', 'Prefeitura Municipal de Cascavel', '75.357.463/0001-43',
 'Serviços de limpeza e conservação predial com dedicação exclusiva',
 '2025-03-01', '2027-02-28', 67200.00, 1612800.00, 'EM_IMPLANTACAO', 15),

('c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', '11111111-1111-1111-1111-111111111111', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
 '09/2024', 'Universidade Federal do Paraná', '75.357.463/0001-43',
 'Apoio administrativo e recepção com dedicação exclusiva de mão de obra',
 '2024-04-01', '2025-03-31', 38100.00, 457200.00, 'ATIVO', 6),

('c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
 '301/2023', 'Instituto Federal do Paraná - Campus Curitiba', '11.223.344/0001-55',
 'Vigilância e controle de acesso — renovação com repactuação',
 '2023-06-01', '2025-05-31', 52400.00, 1257600.00, 'SUSPENSO', 4);

-- ============================================
-- 20 POSTOS DE SERVIÇO (distribuídos nos contratos)
-- ============================================

-- Contrato 45/2024 (Prefeitura Curitiba) — 12 postos
INSERT INTO service_posts (tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome) VALUES
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-001', 'Portaria Principal - Paço Municipal', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Carlos Eduardo Silva'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-002', 'Garagem - Paço Municipal', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Roberto Carlos Mendes'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-003', 'Arquivo Central', 'Vigilante', '5174-10', '5x2', 8, 5420.00, 'ATIVO', 'Fernanda Lima Costa'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-004', 'Recepção - Secretaria de Saúde', 'Recepcionista', '4211-05', '5x2', 8, 3980.00, 'ATIVO', 'Juliana Pereira Santos'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-005', 'Estacionamento - Paço', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'VAGO', NULL),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-006', 'Almoxarifado', 'Auxiliar de Serviços', '5143-20', '5x2', 8, 3650.00, 'ATIVO', 'Marcos Antonio Rocha'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-007', 'Portaria - CRAS Norte', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Ricardo Souza Almeida'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-008', 'Portaria - CRAS Sul', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Paulo Henrique Dias'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-009', 'Vigilância - Central de Compras', 'Vigilante', '5174-10', '5x2', 8, 5420.00, 'ATIVO', 'Ana Clara Mendes'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-010', 'Controle de Acesso - Almoxarifado', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-011', 'Ronda Noturna - Paço', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Daniel Oliveira Nunes'),
('11111111-1111-1111-1111-111111111111', 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1', 'POSTO-012', 'Supervisão Diurna', 'Supervisor de Vigilância', '5174-10', '5x2', 8, 7250.00, 'ATIVO', 'Sérgio Luiz Barbosa');

-- Contrato 112/2024 (Estado do Paraná) — 8 postos
INSERT INTO service_posts (tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome) VALUES
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-001', 'DETRAN - Cascavel', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Lucas Martins'),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-002', 'DETRAN - Foz do Iguaçu', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Bruno Ferreira'),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-003', 'Recepção - Instituto de Identificação', 'Recepcionista', '4211-05', '5x2', 8, 3850.00, 'ATIVO', 'Camila Rodrigues'),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-004', 'Portaria - 6º Batalhão', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'VAGO', NULL),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-005', 'Arquivo - Secretaria de Justiça', 'Auxiliar Administrativo', '4110-05', '5x2', 8, 3720.00, 'ATIVO', 'Patrícia Gomes'),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-006', 'Controle de Acesso - Foz', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Gustavo Lima'),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-007', 'Ronda - Complexo Administrativo', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Rafael Souza'),
('11111111-1111-1111-1111-111111111111', 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2', 'EPR-008', 'Supervisão Regional', 'Supervisor', '5174-10', '5x2', 8, 6800.00, 'ATIVO', 'Marcelo Dias');

-- Contrato 78/2025 (Cascavel - em implantação) — 15 postos (resumido para 4 no seed)
INSERT INTO service_posts (tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome) VALUES
('11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'CAS-001', 'Prefeitura - Paço', 'Auxiliar de Limpeza', '5143-20', '5x2', 8, 2980.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'CAS-002', 'CRAS - Centro', 'Auxiliar de Limpeza', '5143-20', '5x2', 8, 2980.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'CAS-003', 'Secretaria de Educação', 'Supervisor de Limpeza', '5143-20', '5x2', 8, 3450.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'CAS-004', 'Fórum - Limpeza', 'Auxiliar de Limpeza', '5143-20', '5x2', 8, 2980.00, 'AGUARDANDO_IMPLANTACAO', NULL);

-- Contrato 09/2024 (UFPR) — 6 postos
INSERT INTO service_posts (tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome) VALUES
('11111111-1111-1111-1111-111111111111', 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'UFPR-001', 'Reitoria - Portaria', 'Recepcionista', '4211-05', '5x2', 8, 4120.00, 'ATIVO', 'Isabela Martins'),
('11111111-1111-1111-1111-111111111111', 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'UFPR-002', 'Biblioteca Central', 'Auxiliar de Serviços', '5143-20', '5x2', 8, 3650.00, 'ATIVO', 'João Pedro Almeida'),
('11111111-1111-1111-1111-111111111111', 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'UFPR-003', 'Departamento de Matemática', 'Recepcionista', '4211-05', '5x2', 8, 4120.00, 'ATIVO', 'Larissa Costa'),
('11111111-1111-1111-1111-111111111111', 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'UFPR-004', 'Ginásio de Esportes', 'Vigilante', '5174-10', '12x36', 12, 5890.00, 'ATIVO', 'André Luiz Santos'),
('11111111-1111-1111-1111-111111111111', 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'UFPR-005', 'Bloco de Aulas - Setor Leste', 'Vigilante', '5174-10', '12x36', 12, 5890.00, 'VAGO', NULL),
('11111111-1111-1111-1111-111111111111', 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4', 'UFPR-006', 'Supervisão de Recepção', 'Supervisor', '4211-05', '5x2', 8, 4650.00, 'ATIVO', 'Mônica Vasconcelos');

-- Contrato 301/2023 (IFPR - suspenso) — 4 postos
INSERT INTO service_posts (tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome) VALUES
('11111111-1111-1111-1111-111111111111', 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5', 'IFPR-001', 'Portaria Principal', 'Vigilante', '5174-10', '12x36', 12, 5980.00, 'SUSPENSO', 'Antigo: José Carlos'),
('11111111-1111-1111-1111-111111111111', 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5', 'IFPR-002', 'Laboratório - Bloco A', 'Vigilante', '5174-10', '12x36', 12, 5980.00, 'SUSPENSO', 'Antigo: Tiago Ramos'),
('11111111-1111-1111-1111-111111111111', 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5', 'IFPR-003', 'Recepção Administrativa', 'Recepcionista', '4211-05', '5x2', 8, 4100.00, 'SUSPENSO', 'Antigo: Beatriz Nunes'),
('11111111-1111-1111-1111-111111111111', 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5', 'IFPR-004', 'Supervisão', 'Supervisor', '5174-10', '5x2', 8, 6650.00, 'SUSPENSO', 'Antigo: Cláudio Mendes');

-- Atualizar contagem de postos nos contratos (simplificado)
UPDATE contracts SET qtd_postos_contratados = 12 WHERE id = 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1';
UPDATE contracts SET qtd_postos_contratados = 8  WHERE id = 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2';
UPDATE contracts SET qtd_postos_contratados = 15 WHERE id = 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3';
UPDATE contracts SET qtd_postos_contratados = 6  WHERE id = 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4';
UPDATE contracts SET qtd_postos_contratados = 4  WHERE id = 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5';

-- Fim do seed demo
-- Total: 1 tenant, 3 empresas, 2 filiais, 5 contratos, 20+ postos
