-- V45 — Corrige o schema para o ddl-auto: validate do Hibernate.
-- (1) Adiciona colunas de AuditEntity (created_at/updated_at/created_by/updated_by)
--     ausentes em tabelas criadas sem todas elas.
-- (2) Adiciona retencoes_tributarias.darf_gerado (entidade RetencaoTributaria).

ALTER TABLE accounting_periods ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE accounting_periods ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE banco_horas ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE banco_horas ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE bidding_impugnacoes ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE bidding_impugnacoes ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE bidding_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE bidding_items ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE bidding_lots ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE bidding_lots ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE bidding_postos ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE bidding_postos ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE bidding_proposals ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE bidding_proposals ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE branches ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE certificate_vault_refs ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE certificate_vault_refs ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE contract_occurrences ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE contract_occurrences ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE employee_compliance ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE employee_compliance ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE enterprise_groups ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE enterprise_groups ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE equipment_allocations ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE equipment_allocations ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE equipment_maintenance ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE equipment_maintenance ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE glosa_rules ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE glosa_rules ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE glosas ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE glosas ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE ia_approval_queue ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE ia_approval_queue ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE implantation_checklist_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE implantation_checklist_items ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE imr_indicators ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE imr_indicators ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE imr_indicators ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE imr_ranges ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE imr_ranges ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE imr_ranges ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE imr_ranges ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE normalized_punches ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE normalized_punches ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE payslip_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE payslip_items ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE punch_adjustments ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE punch_adjustments ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE raw_punches ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE raw_punches ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE raw_punches ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE service_posts ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE time_clock_devices ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE time_clock_devices ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE winning_spreadsheets ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE winning_spreadsheets ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE retencoes_tributarias ADD COLUMN IF NOT EXISTS darf_gerado TEXT;
