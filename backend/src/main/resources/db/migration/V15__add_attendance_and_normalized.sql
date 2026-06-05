-- V15 - Apuração de Ponto (AttendanceDay + NormalizedPunch)
-- SPEC §9 (Controle de Ponto), §10 (Relógios e AFD/AEJ), §25 (Modelo de dados)
-- Complementa V14 (time_clock_devices + raw_punches)

CREATE TABLE IF NOT EXISTS normalized_punches (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    raw_punch_id        UUID REFERENCES raw_punches(id),
    employee_id         UUID NOT NULL REFERENCES employees(id),
    post_id             UUID REFERENCES service_posts(id),
    contract_id         UUID REFERENCES contracts(id),
    punch_timestamp     TIMESTAMP NOT NULL,
    punch_type          VARCHAR(20) NOT NULL,
    source              VARCHAR(50) DEFAULT 'DEVICE',
    justification       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS attendance_days (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    employee_id             UUID NOT NULL REFERENCES employees(id),
    post_id                 UUID REFERENCES service_posts(id),
    contract_id             UUID REFERENCES contracts(id),
    date                    DATE NOT NULL,
    first_entry             TIMESTAMP,
    last_exit               TIMESTAMP,
    total_worked_minutes    INTEGER DEFAULT 0,
    delay_minutes           INTEGER DEFAULT 0,
    absence_minutes         INTEGER DEFAULT 0,
    source                  VARCHAR(50) DEFAULT 'AUTO_PROCESSED',
    justification           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

-- Indexes para performance de consultas operacionais e de glosa/cobertura
CREATE INDEX IF NOT EXISTS idx_normalized_punches_tenant_time ON normalized_punches(tenant_id, punch_timestamp);
CREATE INDEX IF NOT EXISTS idx_normalized_punches_employee_date ON normalized_punches(tenant_id, employee_id, punch_timestamp);

CREATE INDEX IF NOT EXISTS idx_attendance_days_tenant_date ON attendance_days(tenant_id, date);
CREATE INDEX IF NOT EXISTS idx_attendance_days_employee ON attendance_days(tenant_id, employee_id, date);
CREATE INDEX IF NOT EXISTS idx_attendance_days_contract ON attendance_days(tenant_id, contract_id, date);
CREATE INDEX IF NOT EXISTS idx_attendance_days_post ON attendance_days(tenant_id, post_id, date);

COMMENT ON TABLE normalized_punches IS 'Marcações normalizadas (após pareamento e vinculação) - base para AttendanceDay. SPEC §9';
COMMENT ON TABLE attendance_days IS 'Apuração diária de presença por colaborador/posto. Usada para cobertura, glosas e medição. SPEC §9, §17, §18';

-- Nota: em produção, adicionar constraints de unicidade (tenant + employee + date) + triggers de reprocessamento.
