-- Phase 7: IA approval queue + RAG index metadata

CREATE TABLE IF NOT EXISTS ia_approval_queue (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    agent_name      VARCHAR(80) NOT NULL,
    action_type     VARCHAR(80) NOT NULL,
    entity_type     VARCHAR(80),
    entity_id       UUID,
    contract_id     UUID REFERENCES contracts(id),
    title           VARCHAR(300) NOT NULL,
    summary         TEXT,
    payload         TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_by    VARCHAR(150),
    reviewed_by     VARCHAR(150),
    reviewed_at     TIMESTAMPTZ,
    review_notes    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ia_approval_tenant ON ia_approval_queue(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ia_approval_status ON ia_approval_queue(status);

CREATE TABLE IF NOT EXISTS rag_index_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    doc_key         VARCHAR(200) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    title           VARCHAR(300),
    content         TEXT NOT NULL,
    embedding_stub  TEXT,
    indexed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, doc_key)
);
CREATE INDEX IF NOT EXISTS idx_rag_index_tenant ON rag_index_documents(tenant_id);
