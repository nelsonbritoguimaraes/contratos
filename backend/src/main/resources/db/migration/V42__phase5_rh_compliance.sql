-- V42: RH Phase 5 — compliance (ASO, treinamentos, EPI) + fiscal signatories

CREATE TABLE IF NOT EXISTS employee_compliance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    descricao VARCHAR(255),
    data_realizacao DATE,
    data_validade DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'VALIDO',
    documento_ref VARCHAR(100),
    observacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_employee_compliance_tenant ON employee_compliance (tenant_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_compliance_validade ON employee_compliance (tenant_id, data_validade)
    WHERE data_validade IS NOT NULL;

COMMENT ON TABLE employee_compliance IS 'ASO, treinamentos NR, EPI — compliance trabalhista';
COMMENT ON COLUMN employee_compliance.tipo IS 'ASO, TREINAMENTO, EPI';

ALTER TABLE tenant_fiscal_profile
    ADD COLUMN IF NOT EXISTS razao_social VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contador_nome VARCHAR(150),
    ADD COLUMN IF NOT EXISTS contador_cpf VARCHAR(14),
    ADD COLUMN IF NOT EXISTS contador_crc VARCHAR(20),
    ADD COLUMN IF NOT EXISTS representante_nome VARCHAR(150),
    ADD COLUMN IF NOT EXISTS representante_cpf VARCHAR(14);

CREATE TABLE IF NOT EXISTS contract_occurrences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    data_ocorrencia DATE NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT,
    severidade VARCHAR(20) NOT NULL DEFAULT 'INFO',
    registrado_por VARCHAR(150),
    anexo_ref VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contract_occurrences_contract ON contract_occurrences (tenant_id, contract_id, data_ocorrencia DESC);

COMMENT ON TABLE contract_occurrences IS 'Livro de ocorrências do contrato (Phase 4)';
