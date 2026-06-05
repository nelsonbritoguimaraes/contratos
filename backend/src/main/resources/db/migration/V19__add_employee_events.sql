-- V19 - Módulo de RH: Eventos de Departamento Pessoal
-- SPEC §8.2 Eventos de DP

CREATE TABLE IF NOT EXISTS employee_events (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id             UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    contract_id             UUID REFERENCES contracts(id),
    event_type              VARCHAR(50) NOT NULL,
    event_date              DATE NOT NULL,
    description             TEXT,
    previous_value          NUMERIC(14,2),
    new_value               NUMERIC(14,2),
    reason                  VARCHAR(200),
    document_reference      VARCHAR(100),
    affects_payroll_from    DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_employee_events_tenant_employee ON employee_events(tenant_id, employee_id, event_date);
CREATE INDEX IF NOT EXISTS idx_employee_events_tenant_type ON employee_events(tenant_id, event_type);

COMMENT ON TABLE employee_events IS 'Histórico de eventos de DP do colaborador (admissão, alterações salariais, férias, rescisões, etc.). Fundamental para cálculo de folha e eSocial.';
