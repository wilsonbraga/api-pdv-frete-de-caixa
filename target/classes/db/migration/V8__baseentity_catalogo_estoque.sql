-- ============================================================
-- V6__baseentity_catalogo_estoque.sql
-- Adiciona colunas de auditoria (BaseEntity) nas tabelas:
--   • catalogo_categoria   (criado pelas migrations do catálogo)
--   • produto_catalogo     (criado pelas migrations do catálogo)
--   • movimentacao_estoque (criado no V1, mas incompleto)
-- ============================================================

-- ── catalogo_categoria ───────────────────────────────────────
-- Tabela nova: precisa das 4 colunas
ALTER TABLE catalogo_categoria
    ADD COLUMN IF NOT EXISTS criado_em      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS criado_por     UUID,
    ADD COLUMN IF NOT EXISTS atualizado_por UUID;

-- ── produto_catalogo ─────────────────────────────────────────
-- V5 não cria mais criado_em (removido); adicionar as 4 colunas
ALTER TABLE produto_catalogo
    ADD COLUMN IF NOT EXISTS criado_em      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS criado_por     UUID,
    ADD COLUMN IF NOT EXISTS atualizado_por UUID;

-- ── movimentacao_estoque ─────────────────────────────────────
-- V1 criou criado_em e criado_por; adicionar as demais
ALTER TABLE movimentacao_estoque
    ADD COLUMN IF NOT EXISTS atualizado_em  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS atualizado_por UUID;

-- ── Trigger para manter atualizado_em sincronizado ───────────
-- (igual ao padrão das demais tabelas do sistema)

CREATE OR REPLACE FUNCTION set_atualizado_em()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- catalogo_categoria
DROP TRIGGER IF EXISTS trg_catalogo_categoria_atualizado ON catalogo_categoria;
CREATE TRIGGER trg_catalogo_categoria_atualizado
    BEFORE UPDATE ON catalogo_categoria
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();

-- produto_catalogo
DROP TRIGGER IF EXISTS trg_produto_catalogo_atualizado ON produto_catalogo;
CREATE TRIGGER trg_produto_catalogo_atualizado
    BEFORE UPDATE ON produto_catalogo
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();

-- movimentacao_estoque
DROP TRIGGER IF EXISTS trg_movimentacao_estoque_atualizado ON movimentacao_estoque;
CREATE TRIGGER trg_movimentacao_estoque_atualizado
    BEFORE UPDATE ON movimentacao_estoque
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();
