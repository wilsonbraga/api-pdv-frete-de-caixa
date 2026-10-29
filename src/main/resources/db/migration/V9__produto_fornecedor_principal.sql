-- ============================================================
-- V7__produto_fornecedor_principal.sql
-- Vincula um fornecedor principal (opcional) a cada produto.
-- Usado nos alertas de estoque mínimo para informar de quem
-- o operador deve comprar e gerar link de WhatsApp.
-- ============================================================

ALTER TABLE produto
    ADD COLUMN IF NOT EXISTS fornecedor_id UUID
        REFERENCES fornecedor(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_produto_fornecedor ON produto(fornecedor_id);

COMMENT ON COLUMN produto.fornecedor_id IS
    'Fornecedor principal do produto — opcional. '
    'Usado para alertas de estoque mínimo com dados de contato.';
