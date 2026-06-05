-- V28 — Módulo Financeiro Enterprise (CFO Literal)
-- Tabelas core para Contas a Pagar/Receber, Tesouraria, NFS-e, Fluxo de Caixa e Conciliação
-- Alinhado com SPEC §16 (Medição/Faturamento/NFS-e), §22 (Módulo Financeiro), §25.7 (Faturamento e Financeiro)
-- Padrão: multi-tenant forte (tenant_id em todas as tabelas), AuditEntity, índices otimizados para CFO queries

-- ============================================================
-- 1. CONTAS BANCÁRIAS (Tesouraria)
-- ============================================================
CREATE TABLE contas_bancarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    banco_codigo VARCHAR(10) NOT NULL,           -- 001, 033, 104, etc.
    banco_nome VARCHAR(100) NOT NULL,
    agencia VARCHAR(20) NOT NULL,
    conta VARCHAR(30) NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'CORRENTE', -- CORRENTE, POUPANCA, APLICACAO, CAIXA
    saldo_atual NUMERIC(16,2) NOT NULL DEFAULT 0,
    conta_contabil_id UUID,                       -- link com plano de contas (opcional)
    ativa BOOLEAN NOT NULL DEFAULT true,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_contas_bancarias_tenant_conta UNIQUE (tenant_id, banco_codigo, agencia, conta)
);

CREATE INDEX idx_contas_bancarias_tenant ON contas_bancarias(tenant_id);
CREATE INDEX idx_contas_bancarias_ativa ON contas_bancarias(tenant_id, ativa);
COMMENT ON TABLE contas_bancarias IS 'SPEC §22.3 — Contas bancárias e caixa do tenant (CFO Treasury)';

-- ============================================================
-- 2. CONTAS A RECEBER (AR)
-- ============================================================
CREATE TABLE contas_a_receber (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contrato_id UUID,                             -- FK para contracts (opcional, mas recomendado)
    measurement_id UUID,                          -- origem da medição/faturamento
    nota_fiscal_id UUID,                          -- referência à NFS-e emitida
    valor_bruto NUMERIC(16,2) NOT NULL,
    valor_liquido NUMERIC(16,2) NOT NULL,
    vencimento DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTO', -- ABERTO, PARCIAL, PAGO, VENCIDO, CANCELADO
    dias_atraso INTEGER NOT NULL DEFAULT 0,
    juros_multa NUMERIC(16,2) NOT NULL DEFAULT 0,
    data_recebimento DATE,
    valor_recebido NUMERIC(16,2),
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_contas_a_receber_tenant ON contas_a_receber(tenant_id);
CREATE INDEX idx_contas_a_receber_vencimento ON contas_a_receber(tenant_id, vencimento, status);
CREATE INDEX idx_contas_a_receber_status ON contas_a_receber(tenant_id, status);
CREATE INDEX idx_contas_a_receber_contrato ON contas_a_receber(tenant_id, contrato_id);
COMMENT ON TABLE contas_a_receber IS 'SPEC §22.1 — Contas a Receber (AR) com controle de aging e retenções';

-- ============================================================
-- 3. CONTAS A PAGAR (AP)
-- ============================================================
CREATE TABLE contas_a_pagar (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    origem VARCHAR(50) NOT NULL,                  -- PAYSLIP, RETENCAO_TRIBUTARIA, FORNECEDOR, UNIFORME, EQUIPAMENTO, SERVICO, MANUAL
    origem_id UUID,                               -- payslip_id, retencao_id, etc.
    contrato_id UUID,
    valor NUMERIC(16,2) NOT NULL,
    vencimento DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTO', -- ABERTO, APROVADO, PAGO, VENCIDO, CANCELADO
    data_pagamento DATE,
    valor_pago NUMERIC(16,2),
    forma_pagamento VARCHAR(30),                  -- PIX, TED, BOLETO, DINHEIRO
    conta_bancaria_origem_id UUID,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_contas_a_pagar_tenant ON contas_a_pagar(tenant_id);
CREATE INDEX idx_contas_a_pagar_vencimento ON contas_a_pagar(tenant_id, vencimento, status);
CREATE INDEX idx_contas_a_pagar_origem ON contas_a_pagar(tenant_id, origem, origem_id);
CREATE INDEX idx_contas_a_pagar_status ON contas_a_pagar(tenant_id, status);
COMMENT ON TABLE contas_a_pagar IS 'SPEC §22.2 — Contas a Pagar (AP) — folha, tributos, fornecedores, etc.';

-- ============================================================
-- 4. TRANSAÇÕES FINANCEIRAS (Escrituração do Caixa/Tesouraria)
-- ============================================================
CREATE TABLE transacoes_financeiras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    data DATE NOT NULL,
    conta_bancaria_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,                    -- ENTRADA, SAIDA
    valor NUMERIC(16,2) NOT NULL,
    origem_tipo VARCHAR(50),                      -- RECEBIMENTO, PAGAMENTO_FOLHA, NFS_E, RETENCAO, CONCILIACAO, MANUAL
    origem_id UUID,
    historico TEXT NOT NULL,
    conciliado BOOLEAN NOT NULL DEFAULT false,
    conciliacao_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_transacoes_financeiras_tenant_data ON transacoes_financeiras(tenant_id, data);
CREATE INDEX idx_transacoes_financeiras_conta ON transacoes_financeiras(tenant_id, conta_bancaria_id, data);
CREATE INDEX idx_transacoes_financeiras_conciliado ON transacoes_financeiras(tenant_id, conciliado);
COMMENT ON TABLE transacoes_financeiras IS 'SPEC §22.3 — Movimentação real de caixa (não é lançamento contábil)';

-- ============================================================
-- 5. NOTAS FISCAIS DE SERVIÇO (NFS-e) — Faturamento
-- ============================================================
CREATE TABLE notas_fiscais_servico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    numero VARCHAR(20) NOT NULL,
    serie VARCHAR(10) NOT NULL DEFAULT '1',
    codigo_verificacao VARCHAR(100),
    data_emissao DATE NOT NULL,
    tomador_cnpj VARCHAR(20) NOT NULL,
    tomador_razao_social VARCHAR(200),
    contrato_id UUID,
    measurement_id UUID,
    valor_servicos NUMERIC(16,2) NOT NULL,
    valor_liquido NUMERIC(16,2) NOT NULL,
    iss_retido NUMERIC(16,2) NOT NULL DEFAULT 0,
    outras_retencoes NUMERIC(16,2) NOT NULL DEFAULT 0,
    xml TEXT,                                     -- XML completo da NFS-e (Portal Nacional ou prefeitura)
    pdf_url TEXT,                                 -- stub ou link futuro
    status VARCHAR(30) NOT NULL DEFAULT 'EMITIDA',-- EMITIDA, CANCELADA, SUBSTITUIDA
    protocolo VARCHAR(100),
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_nfs_tenant_numero_serie UNIQUE (tenant_id, numero, serie)
);

CREATE INDEX idx_nfs_tenant ON notas_fiscais_servico(tenant_id);
CREATE INDEX idx_nfs_measurement ON notas_fiscais_servico(tenant_id, measurement_id);
CREATE INDEX idx_nfs_status ON notas_fiscais_servico(tenant_id, status);
COMMENT ON TABLE notas_fiscais_servico IS 'SPEC §16.2 e §22 — NFS-e emitida (Portal Nacional / prefeituras)';

-- ============================================================
-- 6. RETENÇÕES TRIBUTÁRIAS
-- ============================================================
CREATE TABLE retencoes_tributarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nota_fiscal_id UUID,
    conta_a_pagar_id UUID,
    tipo VARCHAR(30) NOT NULL,                    -- ISS, PIS, COFINS, CSLL, IRRF, INSS, OUTROS
    aliquota NUMERIC(5,4) NOT NULL,
    base_calculo NUMERIC(16,2) NOT NULL,
    valor_retido NUMERIC(16,2) NOT NULL,
    codigo_receita VARCHAR(20),                   -- para geração de DARF/GPS
    data_vencimento DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, PAGO, VENCIDO
    dar_f_gerado TEXT,                            -- texto estruturado ou código de barras simulado
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_retencoes_tenant ON retencoes_tributarias(tenant_id);
CREATE INDEX idx_retencoes_nfs ON retencoes_tributarias(tenant_id, nota_fiscal_id);
CREATE INDEX idx_retencoes_vencimento ON retencoes_tributarias(tenant_id, data_vencimento, status);
COMMENT ON TABLE retencoes_tributarias IS 'SPEC §21.2 e §22 — Retenções na fonte (ISS, Federais, INSS)';

-- ============================================================
-- 7. CONCILIAÇÕES BANCÁRIAS
-- ============================================================
CREATE TABLE conciliacoes_bancarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_bancaria_id UUID NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    saldo_extrato NUMERIC(16,2) NOT NULL,
    saldo_sistema NUMERIC(16,2) NOT NULL,
    diferenca NUMERIC(16,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OK',    -- OK, DIVERGENTE, EM_ANALISE
    data_conciliacao DATE NOT NULL DEFAULT CURRENT_DATE,
    observacoes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_conciliacoes_tenant ON conciliacoes_bancarias(tenant_id);
CREATE INDEX idx_conciliacoes_conta_periodo ON conciliacoes_bancarias(tenant_id, conta_bancaria_id, data_inicio, data_fim);
COMMENT ON TABLE conciliacoes_bancarias IS 'SPEC §22.3 — Controle de conciliação bancária';

-- ============================================================
-- 8. EXTRATOS BANCÁRIOS IMPORTADOS (para conciliação)
-- ============================================================
CREATE TABLE extratos_bancarios_itens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_bancaria_id UUID NOT NULL,
    data DATE NOT NULL,
    documento VARCHAR(50),
    historico TEXT NOT NULL,
    valor NUMERIC(16,2) NOT NULL,
    tipo VARCHAR(10) NOT NULL,                    -- CREDITO, DEBITO
    conciliado BOOLEAN NOT NULL DEFAULT false,
    transacao_financeira_id UUID,
    conciliacao_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_extratos_tenant_conta_data ON extratos_bancarios_itens(tenant_id, conta_bancaria_id, data);
CREATE INDEX idx_extratos_conciliado ON extratos_bancarios_itens(tenant_id, conciliado);
COMMENT ON TABLE extratos_bancarios_itens IS 'SPEC §22.3 — Itens importados de extrato (OFX, CSV, manual)';

-- ============================================================
-- 9. PREVISÕES E FLUXO DE CAIXA PROJETADO (CFO Forecasting)
-- ============================================================
CREATE TABLE previsoes_financeiras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    data DATE NOT NULL,
    tipo VARCHAR(50) NOT NULL,                    -- RECEBIMENTO_PROJETADO, PAGAMENTO_FOLHA, TRIBUTO, FORNECEDOR, CAPEX, OUTROS
    valor NUMERIC(16,2) NOT NULL,
    contrato_id UUID,
    probabilidade INTEGER NOT NULL DEFAULT 80,    -- 0 a 100
    cenario VARCHAR(20) NOT NULL DEFAULT 'BASE',  -- BASE, OTIMISTA, PESSIMISTA
    descricao TEXT,
    origem VARCHAR(50),                           -- MANUAL, SISTEMA, SIMULACAO
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_previsoes_tenant_data ON previsoes_financeiras(tenant_id, data, cenario);
CREATE INDEX idx_previsoes_contrato ON previsoes_financeiras(tenant_id, contrato_id);
COMMENT ON TABLE previsoes_financeiras IS 'SPEC §22.3 — Motor de previsão de fluxo de caixa (13 semanas / 12 meses)';

-- ============================================================
-- 10. PAGAMENTOS (Baixas de AP)
-- ============================================================
CREATE TABLE pagamentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_a_pagar_id UUID NOT NULL,
    data DATE NOT NULL,
    valor NUMERIC(16,2) NOT NULL,
    conta_bancaria_id UUID,
    forma_pagamento VARCHAR(30),
    comprovante_url TEXT,
    usuario_aprovador VARCHAR(100),
    nivel_aprovacao INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_pagamentos_tenant ON pagamentos(tenant_id);
CREATE INDEX idx_pagamentos_ap ON pagamentos(tenant_id, conta_a_pagar_id);
COMMENT ON TABLE pagamentos IS 'SPEC §22.2 — Registro de execução de pagamento';

-- ============================================================
-- 11. RECEBIMENTOS (Baixas de AR)
-- ============================================================
CREATE TABLE recebimentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conta_a_receber_id UUID NOT NULL,
    data DATE NOT NULL,
    valor NUMERIC(16,2) NOT NULL,
    conta_bancaria_id UUID,
    nota_fiscal_id UUID,
    retencoes_aplicadas JSONB,                    -- detalhamento das retenções no recebimento
    observacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_recebimentos_tenant ON recebimentos(tenant_id);
CREATE INDEX idx_recebimentos_ar ON recebimentos(tenant_id, conta_a_receber_id);
COMMENT ON TABLE recebimentos IS 'SPEC §22.1 — Registro de recebimento (baixa de AR)';

-- ============================================================
-- 12. FECHAMENTO FINANCEIRO MENSAL (separado do contábil)
-- ============================================================
CREATE TABLE financeiro_periodos_fechamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO', -- ABERTO, FECHADO, REABERTO
    saldo_caixa_inicial NUMERIC(16,2),
    saldo_caixa_final NUMERIC(16,2),
    total_recebimentos NUMERIC(16,2),
    total_pagamentos NUMERIC(16,2),
    total_retencoes NUMERIC(16,2),
    observacoes TEXT,
    fechado_por VARCHAR(100),
    data_fechamento TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_fechamento_tenant_periodo UNIQUE (tenant_id, data_inicio, data_fim)
);

CREATE INDEX idx_fechamento_tenant ON financeiro_periodos_fechamento(tenant_id);
COMMENT ON TABLE financeiro_periodos_fechamento IS 'SPEC §22 — Fechamento financeiro mensal (CFO control)';

-- ============================================================
-- FKs (adicionadas após criação das tabelas para evitar ordem de dependência)
-- ============================================================
ALTER TABLE contas_bancarias
    ADD CONSTRAINT fk_contas_bancarias_conta_contabil
    FOREIGN KEY (conta_contabil_id) REFERENCES contas_contabeis(id) ON DELETE SET NULL;

ALTER TABLE contas_a_receber
    ADD CONSTRAINT fk_car_measurement
    FOREIGN KEY (measurement_id) REFERENCES measurements(id) ON DELETE SET NULL;

ALTER TABLE contas_a_pagar
    ADD CONSTRAINT fk_cap_conta_bancaria
    FOREIGN KEY (conta_bancaria_origem_id) REFERENCES contas_bancarias(id) ON DELETE SET NULL;

ALTER TABLE transacoes_financeiras
    ADD CONSTRAINT fk_trans_conta_bancaria
    FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias(id) ON DELETE RESTRICT;

ALTER TABLE notas_fiscais_servico
    ADD CONSTRAINT fk_nfs_measurement
    FOREIGN KEY (measurement_id) REFERENCES measurements(id) ON DELETE SET NULL;

ALTER TABLE retencoes_tributarias
    ADD CONSTRAINT fk_ret_nfs
    FOREIGN KEY (nota_fiscal_id) REFERENCES notas_fiscais_servico(id) ON DELETE SET NULL;

ALTER TABLE conciliacoes_bancarias
    ADD CONSTRAINT fk_conc_conta_bancaria
    FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias(id) ON DELETE CASCADE;

ALTER TABLE extratos_bancarios_itens
    ADD CONSTRAINT fk_extrato_conta
    FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias(id) ON DELETE CASCADE;

ALTER TABLE pagamentos
    ADD CONSTRAINT fk_pagamento_cap
    FOREIGN KEY (conta_a_pagar_id) REFERENCES contas_a_pagar(id) ON DELETE CASCADE;

ALTER TABLE recebimentos
    ADD CONSTRAINT fk_recebimento_car
    FOREIGN KEY (conta_a_receber_id) REFERENCES contas_a_receber(id) ON DELETE CASCADE;

-- Fim da V28 — base sólida para o módulo financeiro enterprise CFO.