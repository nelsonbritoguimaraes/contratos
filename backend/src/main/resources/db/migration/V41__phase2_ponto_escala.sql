-- Phase 2-3: Escalas, volantes, glosa evidências/recursos, clock sync status

-- Templates de turno (12x36, plantão, comercial)
CREATE TABLE IF NOT EXISTS shift_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id     UUID REFERENCES contracts(id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,
    shift_type      VARCHAR(30) NOT NULL,
    work_hours      INTEGER,
    rest_hours      INTEGER,
    entry_time      TIME,
    exit_time       TIME,
    cycle_days      INTEGER NOT NULL DEFAULT 2,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_shift_templates_tenant ON shift_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_shift_templates_contract ON shift_templates(contract_id);

-- Escala por posto (vincula posto a template e vigência)
CREATE TABLE IF NOT EXISTS post_schedules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    post_id             UUID NOT NULL REFERENCES service_posts(id) ON DELETE CASCADE,
    shift_template_id   UUID REFERENCES shift_templates(id),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    schedule_type       VARCHAR(30) NOT NULL,
    notes               TEXT,
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_post_schedules_contract ON post_schedules(contract_id);
CREATE INDEX IF NOT EXISTS idx_post_schedules_post ON post_schedules(post_id);
CREATE INDEX IF NOT EXISTS idx_post_schedules_effective ON post_schedules(effective_from, effective_to);

-- Escala de colaboradores por posto
CREATE TABLE IF NOT EXISTS employee_rosters (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    post_id             UUID NOT NULL REFERENCES service_posts(id) ON DELETE CASCADE,
    employee_id         UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    post_schedule_id    UUID REFERENCES post_schedules(id),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    role                VARCHAR(30) NOT NULL DEFAULT 'TITULAR',
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_employee_rosters_contract ON employee_rosters(contract_id);
CREATE INDEX IF NOT EXISTS idx_employee_rosters_post ON employee_rosters(post_id);
CREATE INDEX IF NOT EXISTS idx_employee_rosters_employee ON employee_rosters(employee_id);

-- Workflow volante: falta -> volante -> evidência
CREATE TABLE IF NOT EXISTS volante_assignments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id             UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    post_id                 UUID NOT NULL REFERENCES service_posts(id) ON DELETE CASCADE,
    absent_employee_id      UUID NOT NULL REFERENCES employees(id),
    volante_employee_id     UUID REFERENCES employees(id),
    assignment_date         DATE NOT NULL,
    workflow_status         VARCHAR(30) NOT NULL DEFAULT 'FALTA_DETECTADA',
    detected_at             TIMESTAMPTZ,
    assigned_at             TIMESTAMPTZ,
    confirmed_at            TIMESTAMPTZ,
    evidence_at             TIMESTAMPTZ,
    evidence_url            TEXT,
    evidence_notes          TEXT,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_volante_assignments_contract_date ON volante_assignments(contract_id, assignment_date);
CREATE INDEX IF NOT EXISTS idx_volante_assignments_status ON volante_assignments(workflow_status);

-- Evidências de glosa (SPEC §17.6)
CREATE TABLE IF NOT EXISTS glosa_evidences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    glosa_id        UUID NOT NULL REFERENCES glosas(id) ON DELETE CASCADE,
    evidence_type   VARCHAR(50),
    file_url        TEXT,
    description     TEXT,
    submitted_by    VARCHAR(100),
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_glosa_evidences_glosa ON glosa_evidences(glosa_id);

-- Recursos/contestações de glosa (SPEC §17.6)
CREATE TABLE IF NOT EXISTS glosa_appeals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    glosa_id        UUID NOT NULL REFERENCES glosas(id) ON DELETE CASCADE,
    appeal_reason   TEXT NOT NULL,
    appeal_status   VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    submitted_by    VARCHAR(100),
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by     VARCHAR(100),
    reviewed_at     TIMESTAMPTZ,
    review_notes    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_glosa_appeals_glosa ON glosa_appeals(glosa_id);
CREATE INDEX IF NOT EXISTS idx_glosa_appeals_status ON glosa_appeals(appeal_status);

-- Status de sincronização Clock Bridge Agent
CREATE TABLE IF NOT EXISTS clock_sync_status (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    device_id           UUID NOT NULL REFERENCES time_clock_devices(id) ON DELETE CASCADE,
    last_sync_at        TIMESTAMPTZ,
    last_sync_status    VARCHAR(30),
    punches_imported    INTEGER NOT NULL DEFAULT 0,
    errors_count        INTEGER NOT NULL DEFAULT 0,
    error_message       TEXT,
    next_sync_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    UNIQUE (tenant_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_clock_sync_status_device ON clock_sync_status(device_id);

COMMENT ON TABLE shift_templates IS 'Templates de turno (12x36, plantão) — Phase 2';
COMMENT ON TABLE post_schedules IS 'Escalas por posto — Phase 2';
COMMENT ON TABLE employee_rosters IS 'Escala de colaboradores — Phase 2';
COMMENT ON TABLE volante_assignments IS 'Workflow volante falta->volante->evidência — Phase 2';
COMMENT ON TABLE glosa_evidences IS 'Evidências de glosa — Phase 3 SPEC §17.6';
COMMENT ON TABLE glosa_appeals IS 'Recursos de glosa — Phase 3 SPEC §17.6';
COMMENT ON TABLE clock_sync_status IS 'Status sync Clock Bridge Agent — Phase 2';
