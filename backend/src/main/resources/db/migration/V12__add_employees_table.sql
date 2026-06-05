-- V12 - Tabela de Colaboradores (Employees)
-- SPEC v1.0 seções 8 e 25.4

CREATE TABLE IF NOT EXISTS employees (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id          UUID NOT NULL REFERENCES companies(id),
    branch_id           UUID REFERENCES branches(id),

    full_name           VARCHAR(200) NOT NULL,
    cpf                 VARCHAR(14) NOT NULL UNIQUE,
    rg                  VARCHAR(20),
    pis_nis             VARCHAR(20),
    birth_date          DATE,
    gender              VARCHAR(20),
    marital_status      VARCHAR(30),

    email               VARCHAR(150),
    phone               VARCHAR(30),
    address             TEXT,

    bank_code           VARCHAR(10),
    bank_agency         VARCHAR(20),
    bank_account        VARCHAR(30),

    matricula           VARCHAR(50),
    cargo               VARCHAR(100),
    cbo                 VARCHAR(10),
    salary_base         NUMERIC(12,2),
    admission_date      DATE,
    contract_type       VARCHAR(50),
    status              VARCHAR(30) NOT NULL DEFAULT 'ATIVO',

    aso_date            DATE,
    aso_valid_until     DATE,

    sindicato           VARCHAR(150),
    cct_id              UUID,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_employees_tenant ON employees(tenant_id);
CREATE INDEX IF NOT EXISTS idx_employees_company ON employees(company_id);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_cpf ON employees(cpf);

COMMENT ON TABLE employees IS 'Colaboradores - SPEC §8 e §25.4. Núcleo para Ponto, Folha, Alocação e Glosas.';
