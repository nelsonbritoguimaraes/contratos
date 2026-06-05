-- Fase 0: Row Level Security por tenant_id (SPEC §4, §25)
-- Requer: SET app.current_tenant = '<uuid>' por conexão (TenantRlsInterceptor)

CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
    SELECT NULLIF(current_setting('app.current_tenant', true), '')::UUID;
$$ LANGUAGE sql STABLE;

-- Helper: habilita RLS em tabela com tenant_id
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN SELECT unnest(ARRAY[
        'contracts', 'service_posts', 'employees', 'uniform_items', 'uniform_allocations',
        'documents', 'document_versions', 'email_messages', 'email_attachments',
        'contract_implantations', 'implantation_checklist_items',
        'equipment_items', 'equipment_allocations', 'equipment_maintenance',
        'contract_notifications', 'audit_events', 'enterprise_groups', 'certificate_vault_refs'
    ])
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', tbl);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I USING (tenant_id = current_tenant_id() OR current_tenant_id() IS NULL)',
            tbl
        );
    END LOOP;
END $$;
