-- ============================================================
-- V6__fix_configuracao_pix_audit_columns.sql
-- Adiciona colunas de auditoria (BaseEntity) que foram omitidas
-- na migration V5 ao criar a tabela configuracao_pix.
-- Hibernate valida criado_por e atualizado_por no startup.
-- ============================================================

ALTER TABLE configuracao_pix
    ADD COLUMN IF NOT EXISTS criado_por    UUID,
    ADD COLUMN IF NOT EXISTS atualizado_por UUID;
