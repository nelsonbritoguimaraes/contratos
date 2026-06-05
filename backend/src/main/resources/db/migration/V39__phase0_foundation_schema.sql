-- Fase 0: Schema foundation — tabelas ausentes + audit log global + grupo empresarial

-- Grupo empresarial (SPEC §4)
CREATE TABLE IF NOT EXISTS enterprise_groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_enterprise_groups_tenant ON enterprise_groups(tenant_id);

ALTER TABLE companies ADD COLUMN IF NOT EXISTS enterprise_group_id UUID REFERENCES enterprise_groups(id);

-- Cofre certificados (referência — conteúdo em Vault externo)
CREATE TABLE IF NOT EXISTS certificate_vault_refs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id      UUID REFERENCES companies(id),
    cnpj            VARCHAR(18) NOT NULL,
    alias           VARCHAR(100) NOT NULL,
    vault_path      VARCHAR(500) NOT NULL,
    cert_type       VARCHAR(10) NOT NULL DEFAULT 'A1',
    expires_at      DATE,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_cert_vault_tenant ON certificate_vault_refs(tenant_id);

-- Audit log global imutável (SPEC §24)
CREATE TABLE IF NOT EXISTS audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       UUID,
    action          VARCHAR(80) NOT NULL,
    actor           VARCHAR(150),
    details         TEXT,
    ip_address      VARCHAR(45),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_events_tenant ON audit_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_occurred ON audit_events(occurred_at DESC);

-- Uniformes (SPEC §14)
CREATE TABLE IF NOT EXISTS uniform_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(50),
    size        VARCHAR(20),
    cost        NUMERIC(10,2),
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_uniform_items_tenant ON uniform_items(tenant_id);

CREATE TABLE IF NOT EXISTS uniform_allocations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    uniform_item_id UUID NOT NULL REFERENCES uniform_items(id),
    employee_id     UUID REFERENCES employees(id),
    post_id         UUID REFERENCES service_posts(id),
    quantity        INTEGER NOT NULL DEFAULT 1,
    delivery_date   DATE,
    return_date     DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'DELIVERED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_uniform_alloc_tenant ON uniform_allocations(tenant_id);

-- Documentos (SPEC §15)
CREATE TABLE IF NOT EXISTS documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    file_path       VARCHAR(500),
    mime_type       VARCHAR(100),
    file_size       BIGINT,
    expiry_date     DATE,
    version         VARCHAR(20) NOT NULL DEFAULT '1.0',
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_documents_tenant ON documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_entity ON documents(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS document_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version         VARCHAR(20) NOT NULL,
    file_path       VARCHAR(500),
    extracted_text  TEXT,
    is_current      BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_doc_versions_doc ON document_versions(document_id);

-- E-mail (SPEC §15.2)
CREATE TABLE IF NOT EXISTS email_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    from_address        VARCHAR(200),
    subject             VARCHAR(300),
    body                TEXT,
    received_at         TIMESTAMPTZ,
    classification      VARCHAR(50),
    linked_entity_type  VARCHAR(50),
    linked_entity_id    UUID,
    status              VARCHAR(30) NOT NULL DEFAULT 'RECEBIDO',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_email_messages_tenant ON email_messages(tenant_id);

CREATE TABLE IF NOT EXISTS email_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email_id        UUID NOT NULL REFERENCES email_messages(id) ON DELETE CASCADE,
    file_name       VARCHAR(255),
    file_path       VARCHAR(500),
    mime_type       VARCHAR(100),
    file_size       BIGINT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_email_attachments_email ON email_attachments(email_id);

-- Implantação contratual (SPEC §12)
CREATE TABLE IF NOT EXISTS contract_implantations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id     UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    status          VARCHAR(30) NOT NULL DEFAULT 'PLANEJAMENTO',
    start_date      DATE,
    target_date     DATE,
    operational_date DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    UNIQUE (contract_id)
);
CREATE INDEX IF NOT EXISTS idx_implantations_tenant ON contract_implantations(tenant_id);

CREATE TABLE IF NOT EXISTS implantation_checklist_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    implantation_id     UUID NOT NULL REFERENCES contract_implantations(id) ON DELETE CASCADE,
    code                VARCHAR(50) NOT NULL,
    description         VARCHAR(300) NOT NULL,
    required            BOOLEAN NOT NULL DEFAULT true,
    completed           BOOLEAN NOT NULL DEFAULT false,
    completed_at        TIMESTAMPTZ,
    completed_by        VARCHAR(100),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_checklist_impl ON implantation_checklist_items(implantation_id);

-- Equipamentos (SPEC §13)
CREATE TABLE IF NOT EXISTS equipment_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    serial_number   VARCHAR(100),
    category        VARCHAR(80),
    acquisition_cost NUMERIC(14,2),
    acquisition_date DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'DISPONIVEL',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_equipment_tenant ON equipment_items(tenant_id);

CREATE TABLE IF NOT EXISTS equipment_allocations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    equipment_id    UUID NOT NULL REFERENCES equipment_items(id),
    post_id         UUID REFERENCES service_posts(id),
    employee_id     UUID REFERENCES employees(id),
    contract_id     UUID REFERENCES contracts(id),
    allocated_at    DATE NOT NULL DEFAULT CURRENT_DATE,
    returned_at     DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_equipment_alloc_tenant ON equipment_allocations(tenant_id);

CREATE TABLE IF NOT EXISTS equipment_maintenance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    equipment_id    UUID NOT NULL REFERENCES equipment_items(id),
    maintenance_type VARCHAR(50) NOT NULL,
    description     TEXT,
    cost            NUMERIC(12,2),
    performed_at    DATE NOT NULL,
    next_due_date   DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_equipment_maint ON equipment_maintenance(equipment_id);

-- Notificações contratuais (SPEC §15.1)
CREATE TABLE IF NOT EXISTS contract_notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    contract_id         UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    notification_number VARCHAR(80),
    orgao               VARCHAR(255),
    subject             VARCHAR(300) NOT NULL,
    description         TEXT,
    received_at         DATE NOT NULL,
    response_deadline   DATE,
    responded_at        DATE,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    linked_glosa_id     UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_contract_notif_tenant ON contract_notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contract_notif_contract ON contract_notifications(contract_id);
