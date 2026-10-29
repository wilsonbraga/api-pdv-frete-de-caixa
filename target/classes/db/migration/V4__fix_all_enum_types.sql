-- =============================================================================
-- V4__fix_all_enum_types.sql
--
-- Converte todas as colunas com custom ENUM do PostgreSQL para VARCHAR,
-- resolvendo incompatibilidade com @Enumerated(EnumType.STRING) do Hibernate.
--
-- ESTRATÉGIA:
-- 1. Remove os DEFAULT de cada coluna (eles referenciam o tipo ENUM)
-- 2. Converte a coluna para VARCHAR mantendo os dados
-- 3. Restaura o DEFAULT agora como string literal simples
-- 4. Dropa os tipos ENUM que não são mais necessários
-- =============================================================================


-- ── TABELA: usuario ───────────────────────────────────────────────────────────
ALTER TABLE usuario ALTER COLUMN role DROP DEFAULT;
ALTER TABLE usuario ALTER COLUMN role TYPE VARCHAR(20) USING role::VARCHAR;
ALTER TABLE usuario ALTER COLUMN role SET DEFAULT 'OPERADOR';


-- ── TABELA: caixa ─────────────────────────────────────────────────────────────
-- status_caixa já foi convertido na V3, mas o DEFAULT pode ainda referenciar o tipo.
ALTER TABLE caixa ALTER COLUMN status DROP DEFAULT;
ALTER TABLE caixa ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;
ALTER TABLE caixa ALTER COLUMN status SET DEFAULT 'ABERTO';


-- ── TABELA: movimentacao_caixa ────────────────────────────────────────────────
ALTER TABLE movimentacao_caixa ALTER COLUMN tipo TYPE VARCHAR(30) USING tipo::VARCHAR;


-- ── TABELA: venda ─────────────────────────────────────────────────────────────
ALTER TABLE venda ALTER COLUMN status DROP DEFAULT;
ALTER TABLE venda ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;
ALTER TABLE venda ALTER COLUMN status SET DEFAULT 'ABERTA';


-- ── TABELA: pagamento_venda ───────────────────────────────────────────────────
ALTER TABLE pagamento_venda ALTER COLUMN forma TYPE VARCHAR(20) USING forma::VARCHAR;

ALTER TABLE pagamento_venda ALTER COLUMN status DROP DEFAULT;
ALTER TABLE pagamento_venda ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;
ALTER TABLE pagamento_venda ALTER COLUMN status SET DEFAULT 'PENDENTE';


-- ── TABELA: compra ────────────────────────────────────────────────────────────
ALTER TABLE compra ALTER COLUMN status DROP DEFAULT;
ALTER TABLE compra ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;
ALTER TABLE compra ALTER COLUMN status SET DEFAULT 'PEDIDO';


-- ── TABELA: movimentacao_estoque ──────────────────────────────────────────────
ALTER TABLE movimentacao_estoque ALTER COLUMN tipo TYPE VARCHAR(30) USING tipo::VARCHAR;


-- ── TABELA: conta_financeira ──────────────────────────────────────────────────
ALTER TABLE conta_financeira ALTER COLUMN tipo TYPE VARCHAR(20) USING tipo::VARCHAR;

ALTER TABLE conta_financeira ALTER COLUMN status DROP DEFAULT;
ALTER TABLE conta_financeira ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;
ALTER TABLE conta_financeira ALTER COLUMN status SET DEFAULT 'ABERTA';


-- ── TABELA: fila_sincronizacao ────────────────────────────────────────────────
ALTER TABLE fila_sincronizacao ALTER COLUMN operacao TYPE VARCHAR(10) USING operacao::VARCHAR;

ALTER TABLE fila_sincronizacao ALTER COLUMN status DROP DEFAULT;
ALTER TABLE fila_sincronizacao ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR;
ALTER TABLE fila_sincronizacao ALTER COLUMN status SET DEFAULT 'PENDENTE';


-- =============================================================================
-- Dropa os custom ENUM types — agora sem dependências
-- =============================================================================
DROP TYPE IF EXISTS role_usuario;
DROP TYPE IF EXISTS status_caixa;
DROP TYPE IF EXISTS tipo_movimentacao_caixa;
DROP TYPE IF EXISTS status_venda;
DROP TYPE IF EXISTS forma_pagamento;
DROP TYPE IF EXISTS status_pagamento;
DROP TYPE IF EXISTS status_compra;
DROP TYPE IF EXISTS tipo_movimentacao_estoque;
DROP TYPE IF EXISTS status_sync;
DROP TYPE IF EXISTS operacao_sync;
DROP TYPE IF EXISTS status_conta;
DROP TYPE IF EXISTS tipo_conta;