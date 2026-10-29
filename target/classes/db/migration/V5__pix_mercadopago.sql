-- ============================================================
-- V3__pix_mercadopago.sql
-- Tabelas para integração PIX Mercado Pago
-- ============================================================

CREATE TYPE provedor_pix AS ENUM ('MERCADOPAGO', 'BRADESCO');
CREATE TYPE status_cobranca_pix AS ENUM ('PENDENTE', 'PAGO', 'EXPIRADO', 'CANCELADO', 'ERRO');

-- ===== CONFIGURAÇÃO PIX POR TENANT =====
-- Cada tenant cadastra suas próprias credenciais do MP
CREATE TABLE configuracao_pix (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    provedor        provedor_pix NOT NULL DEFAULT 'MERCADOPAGO',
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,

    -- Mercado Pago
    mp_access_token VARCHAR(500),   -- Token de produção ou sandbox
    mp_public_key   VARCHAR(255),
    mp_user_id      VARCHAR(50),    -- ID da conta MP (preenchido automaticamente)

    -- Chave PIX da conta
    chave_pix       VARCHAR(150),
    tipo_chave      VARCHAR(20),    -- CPF, CNPJ, EMAIL, TELEFONE, ALEATORIA

    -- Identificação
    nome_beneficiario VARCHAR(150), -- Nome que aparece no QR Code
    descricao_padrao  VARCHAR(255), -- Ex: "PDV São João - Pagamento"

    -- Ambiente
    sandbox         BOOLEAN NOT NULL DEFAULT TRUE, -- true = teste, false = produção

    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (tenant_id, provedor)
);

-- ===== COBRANÇA PIX =====
-- Gerada a cada venda que usa PIX
CREATE TABLE cobranca_pix (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    venda_id            UUID REFERENCES venda(id),
    configuracao_pix_id UUID NOT NULL REFERENCES configuracao_pix(id),

    -- Identificadores externos
    mp_payment_id       VARCHAR(100) UNIQUE,    -- ID do pagamento no MP
    mp_preference_id    VARCHAR(100),           -- ID da preferência MP

    -- Dados da cobrança
    valor               NUMERIC(12,2) NOT NULL,
    descricao           VARCHAR(255),

    -- QR Code gerado pelo MP
    qr_code             TEXT,                   -- Código copia e cola (PIX payload)
    qr_code_base64      TEXT,                   -- Imagem do QR Code em base64
    ticket_url          VARCHAR(500),           -- URL do boleto/QR do MP

    -- Status e rastreamento
    status              status_cobranca_pix NOT NULL DEFAULT 'PENDENTE',
    expiracao           TIMESTAMPTZ NOT NULL,
    pago_em             TIMESTAMPTZ,
    cancelado_em        TIMESTAMPTZ,
    erro_mensagem       TEXT,

    -- Dados retornados pelo webhook do MP
    webhook_payload     JSONB,

    device_id           VARCHAR(100),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ===== ÍNDICES =====
CREATE INDEX idx_config_pix_tenant ON configuracao_pix(tenant_id);
CREATE INDEX idx_cobranca_pix_tenant ON cobranca_pix(tenant_id);
CREATE INDEX idx_cobranca_pix_venda ON cobranca_pix(venda_id);
CREATE INDEX idx_cobranca_pix_mp_payment ON cobranca_pix(mp_payment_id);
CREATE INDEX idx_cobranca_pix_status ON cobranca_pix(status);
CREATE INDEX idx_cobranca_pix_expiracao ON cobranca_pix(expiracao) WHERE status = 'PENDENTE';
