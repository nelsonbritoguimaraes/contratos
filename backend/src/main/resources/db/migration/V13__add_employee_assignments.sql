-- V13 - Tabela de Alocações de Colaboradores (EmployeeAssignment)
-- Liga Employee com Postos/Contratos

CREATE TABLE IF NOT EXISTS employee_assignments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id     UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    contract_id     UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    post_id         UUID REFERENCES service_posts(id),
    role            VARCHAR(50),
    start_date      DATE,
    end_date        DATE,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_employee_assignments_tenant ON employee_assignments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_employee_assignments_employee ON employee_assignments(employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_assignments_contract ON employee_assignments(contract_id);
CREATE INDEX IF NOT EXISTS idx_employee_assignments_post ON employee_assignments(post_id);

COMMENT ON TABLE employee_assignments IS 'Alocações de colaboradores em postos/contratos - fundamental para cobertura e ponto.';
