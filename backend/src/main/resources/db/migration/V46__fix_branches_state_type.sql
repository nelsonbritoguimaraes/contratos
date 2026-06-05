-- V46 — Corrige o tipo de branches.state para o ddl-auto: validate.
-- A migração criou state como CHAR(2) (bpchar), mas a entidade Branch mapeia
-- String com @Column(length = 2), que o Hibernate valida como VARCHAR(2).
ALTER TABLE branches ALTER COLUMN state TYPE VARCHAR(2);
