-- V27 - Seed completo do Plano de Contas (para empresas de mão de obra)
-- Adequado para o contexto do ContractOps (contratos, folha, medição, glosas)

-- ATIVO
INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '1', 'ATIVO', 'ATIVO', 'DEVEDORA', 1, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '1.1', 'Ativo Circulante', 'ATIVO', 'DEVEDORA', 2, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '1.1.01', 'Caixa e Bancos', 'ATIVO', 'DEVEDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '1.1.02', 'Clientes', 'ATIVO', 'DEVEDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '1.1.03', 'Adiantamentos a Empregados', 'ATIVO', 'DEVEDORA', 3, true, true FROM tenants;

-- PASSIVO
INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '2', 'PASSIVO', 'PASSIVO', 'CREDORA', 1, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '2.1', 'Passivo Circulante', 'PASSIVO', 'CREDORA', 2, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '2.1.01', 'Salários e Ordenados a Pagar', 'PASSIVO', 'CREDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '2.1.02', 'FGTS a Recolher', 'PASSIVO', 'CREDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '2.1.03', 'INSS a Recolher', 'PASSIVO', 'CREDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '2.1.04', 'IRRF a Recolher', 'PASSIVO', 'CREDORA', 3, true, true FROM tenants;

-- DESPESA
INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3', 'DESPESAS', 'DESPESA', 'DEVEDORA', 1, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.1', 'Despesas com Pessoal', 'DESPESA', 'DEVEDORA', 2, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.1.01', 'Salários e Ordenados', 'DESPESA', 'DEVEDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.1.02', 'Encargos Sociais (INSS e FGTS)', 'DESPESA', 'DEVEDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.1.03', 'Provisão de Férias', 'DESPESA', 'DEVEDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.1.04', 'Provisão de 13º Salário', 'DESPESA', 'DEVEDORA', 3, true, true FROM tenants;

-- RECEITA
INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '4', 'RECEITAS', 'RECEITA', 'CREDORA', 1, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '4.1', 'Receita de Serviços', 'RECEITA', 'CREDORA', 2, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '4.1.01', 'Receita de Contratos de Mão de Obra', 'RECEITA', 'CREDORA', 3, true, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '4.1.02', 'Receita de Administração de Contratos', 'RECEITA', 'CREDORA', 3, true, true FROM tenants;

-- GLOSAS (como despesa ou redução de receita)
INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.2', 'Despesas com Glosas e Penalidades', 'DESPESA', 'DEVEDORA', 2, false, true FROM tenants;

INSERT INTO contas_contabeis (tenant_id, codigo, descricao, tipo, natureza, nivel, aceita_lancamento, ativa) 
SELECT id, '3.2.01', 'Glosas Aplicadas por Contratantes', 'DESPESA', 'DEVEDORA', 3, true, true FROM tenants;
