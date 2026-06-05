-- ============================================================
-- V1.1 — Fundação demo (tenant + empresas + filiais + contratos + postos)
-- ============================================================
-- Estes dados eram criados pelo seed do infra (infra/db/init/02-seed-dev.sql),
-- que só roda no init do Postgres em ambiente local. Em deploys gerenciados
-- (Coolify) esse init não existe, então a fundação é provida aqui via Flyway.
--
-- As migrações seed posteriores (V8 licitações, V23/V24 RH, V29 financeiro) e a
-- V34 (rubricas de ponto) referenciam o tenant fixo 11111111-... e os contratos
-- c1..c5; sem esta fundação elas violam FK para tenants/contracts.
--
-- Idempotente (ON CONFLICT / NOT EXISTS) para tolerar re-execução/repair.

-- Tenant principal (Grupo empresarial)
INSERT INTO tenants (id, name, slug) VALUES
('11111111-1111-1111-1111-111111111111', 'Grupo Segurança Brasil Participações S.A.', 'grupo-seguranca-brasil')
ON CONFLICT (id) DO NOTHING;

-- Empresas (CNPJs fictícios mas com formato válido)
INSERT INTO companies (id, tenant_id, cnpj, razao_social, nome_fantasia) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '12.345.678/0001-90', 'Vigilância Alpha Ltda', 'Alpha Vigilância'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', '23.456.789/0001-01', 'Limpeza Profissional Beta EIRELI', 'Beta Limpeza'),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', '34.567.890/0001-12', 'Serviços Integrados Gama Ltda', 'Gama Serviços')
ON CONFLICT (id) DO NOTHING;

-- Filiais
INSERT INTO branches (id, tenant_id, company_id, name, city, state) VALUES
('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', '11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Matriz Curitiba', 'Curitiba', 'PR'),
('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Operação Oeste', 'Cascavel', 'PR')
ON CONFLICT (id) DO NOTHING;

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
 '2023-06-01', '2025-05-31', 52400.00, 1257600.00, 'SUSPENSO', 4)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- POSTOS DE SERVIÇO (distribuídos nos contratos)
-- ============================================
INSERT INTO service_posts (tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome)
SELECT v.tenant_id, v.contract_id, v.codigo, v.nome, v.funcao, v.cbo, v.escala, v.jornada_horas, v.valor_mensal, v.status, v.titular_nome
FROM (VALUES
-- Contrato 45/2024 (Prefeitura Curitiba) — 12 postos
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-001', 'Portaria Principal - Paço Municipal', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Carlos Eduardo Silva'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-002', 'Garagem - Paço Municipal', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Roberto Carlos Mendes'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-003', 'Arquivo Central', 'Vigilante', '5174-10', '5x2', 8, 5420.00, 'ATIVO', 'Fernanda Lima Costa'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-004', 'Recepção - Secretaria de Saúde', 'Recepcionista', '4211-05', '5x2', 8, 3980.00, 'ATIVO', 'Juliana Pereira Santos'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-005', 'Estacionamento - Paço', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'VAGO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-006', 'Almoxarifado', 'Auxiliar de Serviços', '5143-20', '5x2', 8, 3650.00, 'ATIVO', 'Marcos Antonio Rocha'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-007', 'Portaria - CRAS Norte', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Ricardo Souza Almeida'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-008', 'Portaria - CRAS Sul', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Paulo Henrique Dias'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-009', 'Vigilância - Central de Compras', 'Vigilante', '5174-10', '5x2', 8, 5420.00, 'ATIVO', 'Ana Clara Mendes'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-010', 'Controle de Acesso - Almoxarifado', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-011', 'Ronda Noturna - Paço', 'Vigilante', '5174-10', '12x36', 12, 6850.00, 'ATIVO', 'Daniel Oliveira Nunes'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1'::uuid, 'POSTO-012', 'Supervisão Diurna', 'Supervisor de Vigilância', '5174-10', '5x2', 8, 7250.00, 'ATIVO', 'Sérgio Luiz Barbosa'),
-- Contrato 112/2024 (Estado do Paraná) — 8 postos
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-001', 'DETRAN - Cascavel', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Lucas Martins'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-002', 'DETRAN - Foz do Iguaçu', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Bruno Ferreira'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-003', 'Recepção - Instituto de Identificação', 'Recepcionista', '4211-05', '5x2', 8, 3850.00, 'ATIVO', 'Camila Rodrigues'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-004', 'Portaria - 6º Batalhão', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'VAGO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-005', 'Arquivo - Secretaria de Justiça', 'Auxiliar Administrativo', '4110-05', '5x2', 8, 3720.00, 'ATIVO', 'Patrícia Gomes'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-006', 'Controle de Acesso - Foz', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Gustavo Lima'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-007', 'Ronda - Complexo Administrativo', 'Vigilante', '5174-10', '12x36', 12, 6120.00, 'ATIVO', 'Rafael Souza'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c2c2c2c2-c2c2-c2c2-c2c2-c2c2c2c2c2c2'::uuid, 'EPR-008', 'Supervisão Regional', 'Supervisor', '5174-10', '5x2', 8, 6800.00, 'ATIVO', 'Marcelo Dias'),
-- Contrato 78/2025 (Cascavel - em implantação) — 4 postos
('11111111-1111-1111-1111-111111111111'::uuid, 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3'::uuid, 'CAS-001', 'Prefeitura - Paço', 'Auxiliar de Limpeza', '5143-20', '5x2', 8, 2980.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3'::uuid, 'CAS-002', 'CRAS - Centro', 'Auxiliar de Limpeza', '5143-20', '5x2', 8, 2980.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3'::uuid, 'CAS-003', 'Secretaria de Educação', 'Supervisor de Limpeza', '5143-20', '5x2', 8, 3450.00, 'AGUARDANDO_IMPLANTACAO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3'::uuid, 'CAS-004', 'Fórum - Limpeza', 'Auxiliar de Limpeza', '5143-20', '5x2', 8, 2980.00, 'AGUARDANDO_IMPLANTACAO', NULL),
-- Contrato 09/2024 (UFPR) — 6 postos
('11111111-1111-1111-1111-111111111111'::uuid, 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4'::uuid, 'UFPR-001', 'Reitoria - Portaria', 'Recepcionista', '4211-05', '5x2', 8, 4120.00, 'ATIVO', 'Isabela Martins'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4'::uuid, 'UFPR-002', 'Biblioteca Central', 'Auxiliar de Serviços', '5143-20', '5x2', 8, 3650.00, 'ATIVO', 'João Pedro Almeida'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4'::uuid, 'UFPR-003', 'Departamento de Matemática', 'Recepcionista', '4211-05', '5x2', 8, 4120.00, 'ATIVO', 'Larissa Costa'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4'::uuid, 'UFPR-004', 'Ginásio de Esportes', 'Vigilante', '5174-10', '12x36', 12, 5890.00, 'ATIVO', 'André Luiz Santos'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4'::uuid, 'UFPR-005', 'Bloco de Aulas - Setor Leste', 'Vigilante', '5174-10', '12x36', 12, 5890.00, 'VAGO', NULL),
('11111111-1111-1111-1111-111111111111'::uuid, 'c4c4c4c4-c4c4-c4c4-c4c4-c4c4c4c4c4c4'::uuid, 'UFPR-006', 'Supervisão de Recepção', 'Supervisor', '4211-05', '5x2', 8, 4650.00, 'ATIVO', 'Mônica Vasconcelos'),
-- Contrato 301/2023 (IFPR - suspenso) — 4 postos
('11111111-1111-1111-1111-111111111111'::uuid, 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5'::uuid, 'IFPR-001', 'Portaria Principal', 'Vigilante', '5174-10', '12x36', 12, 5980.00, 'SUSPENSO', 'Antigo: José Carlos'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5'::uuid, 'IFPR-002', 'Laboratório - Bloco A', 'Vigilante', '5174-10', '12x36', 12, 5980.00, 'SUSPENSO', 'Antigo: Tiago Ramos'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5'::uuid, 'IFPR-003', 'Recepção Administrativa', 'Recepcionista', '4211-05', '5x2', 8, 4100.00, 'SUSPENSO', 'Antigo: Beatriz Nunes'),
('11111111-1111-1111-1111-111111111111'::uuid, 'c5c5c5c5-c5c5-c5c5-c5c5-c5c5c5c5c5c5'::uuid, 'IFPR-004', 'Supervisão', 'Supervisor', '5174-10', '5x2', 8, 6650.00, 'SUSPENSO', 'Antigo: Cláudio Mendes')
) AS v(tenant_id, contract_id, codigo, nome, funcao, cbo, escala, jornada_horas, valor_mensal, status, titular_nome)
WHERE NOT EXISTS (
    SELECT 1 FROM service_posts sp WHERE sp.tenant_id = v.tenant_id AND sp.codigo = v.codigo
);
