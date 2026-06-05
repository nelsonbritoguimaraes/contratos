-- V14 - Módulo de Ponto (TimeClock + RawPunches)
-- SPEC §9 e §10 (Portaria 671/2021)

CREATE TABLE IF NOT EXISTS time_clock_devices (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID REFERENCES contracts(id),
    name                VARCHAR(150) NOT NULL,
    manufacturer        VARCHAR(100),
    model               VARCHAR(100),
    serial_number       VARCHAR(100),
    device_type         VARCHAR(50),
    ip_address          VARCHAR(50),
    api_url             TEXT,
    last_sync_at        TIMESTAMPTZ,
    status              VARCHAR(30) DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS raw_punches (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    device_id           UUID REFERENCES time_clock_devices(id),
    employee_id         UUID REFERENCES employees(id),
    matricula           VARCHAR(50),
    punch_timestamp     TIMESTAMP NOT NULL,
    punch_type          VARCHAR(20),
    nsr                 VARCHAR(50),
    raw_data            TEXT,
    import_batch_id     UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_time_clock_tenant ON time_clock_devices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_raw_punches_tenant_time ON raw_punches(tenant_id, punch_timestamp);
CREATE INDEX IF NOT EXISTS idx_raw_punches_device_nsr ON raw_punches(device_id, nsr);

COMMENT ON TABLE time_clock_devices IS 'Relógios de ponto e sistemas alternativos - SPEC §10';
COMMENT ON TABLE raw_punches IS 'Marcações brutas (antes de normalização) - base para AFD/AEJ';
