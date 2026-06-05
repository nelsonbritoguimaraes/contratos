-- V21 - Módulo de RH: Eventos eSocial (stubs)
-- SPEC Fase 3

CREATE TABLE IF NOT EXISTS esocial_events (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id         UUID REFERENCES employees(id),
    event_type          VARCHAR(20) NOT NULL,           -- S2200, S2299, S1200, S1010...
    competence          DATE,
    payload             TEXT,                           -- JSON/XML estruturado (stub)
    status              VARCHAR(30) DEFAULT 'PENDING',  -- PENDING, GENERATED, SENT...
    generated_at        TIMESTAMPTZ,
    receipt_number      VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_esocial_tenant_type ON esocial_events(tenant_id, event_type);
CREATE INDEX IF NOT EXISTS idx_esocial_employee ON esocial_events(tenant_id, employee_id);

COMMENT ON TABLE esocial_events IS 'Eventos eSocial (stubs de alta qualidade). Estrutura pronta para integração real futura.';
