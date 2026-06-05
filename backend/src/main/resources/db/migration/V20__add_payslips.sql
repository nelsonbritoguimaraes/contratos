-- V20 - Módulo de RH: Payslips (Holerites) e Itens
-- SPEC Fase 3 - Folha de Pagamento

CREATE TABLE IF NOT EXISTS payslips (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id           UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    contract_id           UUID NOT NULL REFERENCES contracts(id),
    competence            DATE NOT NULL,
    base_salary           NUMERIC(14,2),
    total_earnings        NUMERIC(14,2) DEFAULT 0,
    total_deductions      NUMERIC(14,2) DEFAULT 0,
    net_amount            NUMERIC(14,2) DEFAULT 0,
    status                VARCHAR(30) DEFAULT 'DRAFT',
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS payslip_items (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    payslip_id            UUID NOT NULL REFERENCES payslips(id) ON DELETE CASCADE,
    rubric_id             UUID NOT NULL REFERENCES payroll_rubrics(id),
    description           VARCHAR(200),
    quantity              NUMERIC(10,2) DEFAULT 1,
    unit_value            NUMERIC(14,2),
    total_value           NUMERIC(14,2) NOT NULL,
    type                  VARCHAR(20),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payslips_tenant_competence ON payslips(tenant_id, competence);
CREATE INDEX IF NOT EXISTS idx_payslips_employee ON payslips(tenant_id, employee_id, competence);
CREATE INDEX IF NOT EXISTS idx_payslip_items_payslip ON payslip_items(payslip_id);

COMMENT ON TABLE payslips IS 'Holerites / Folhas de pagamento calculadas por competência.';
COMMENT ON TABLE payslip_items IS 'Itens/linhas do holerite (valores calculados por rubrica).';
