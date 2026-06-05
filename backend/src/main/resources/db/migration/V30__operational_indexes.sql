-- Índices compostos para consultas operacionais por tenant + contrato + período (SPEC §25)

CREATE INDEX IF NOT EXISTS idx_glosas_tenant_contract_period
    ON glosas (tenant_id, contract_id, measurement_period);

CREATE INDEX IF NOT EXISTS idx_measurements_tenant_contract_period
    ON measurements (tenant_id, contract_id, period);

CREATE INDEX IF NOT EXISTS idx_attendance_days_tenant_contract_date
    ON attendance_days (tenant_id, contract_id, date);
