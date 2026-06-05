-- SPEC §25.8 — PayrollPeriod, férias, benefícios, volantes

CREATE TABLE payroll_periods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    contract_id     UUID,
    competence      DATE NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    closed_at       TIMESTAMPTZ,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    UNIQUE (tenant_id, contract_id, competence)
);

CREATE INDEX idx_payroll_periods_tenant_comp ON payroll_periods (tenant_id, competence);

CREATE TABLE employee_vacations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    employee_id     UUID NOT NULL,
    contract_id     UUID,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    days_count      INT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_employee_vacations_tenant ON employee_vacations (tenant_id, employee_id);

CREATE TABLE employee_benefits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    employee_id     UUID NOT NULL,
    benefit_type    VARCHAR(50) NOT NULL,
    description     VARCHAR(200),
    monthly_value   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_employee_benefits_tenant ON employee_benefits (tenant_id, employee_id);

CREATE TABLE float_workers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    employee_id         UUID NOT NULL,
    region              VARCHAR(100),
    enabled_contracts   TEXT,
    enabled_functions   TEXT,
    availability_notes  TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'DISPONIVEL',
    sla_minutes         INT DEFAULT 60,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    UNIQUE (tenant_id, employee_id)
);

CREATE INDEX idx_float_workers_tenant ON float_workers (tenant_id, status);
