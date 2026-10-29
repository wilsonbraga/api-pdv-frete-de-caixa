-- ============================================================
-- V1__schema_inicial.sql
-- PDV Offline SaaS - Schema inicial completo
-- ============================================================

-- ===== EXTENSÕES =====
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- ===== ENUM TYPES =====
CREATE TYPE role_usuario AS ENUM ('ADMIN', 'GERENTE', 'OPERADOR', 'FINANCEIRO');
CREATE TYPE status_caixa AS ENUM ('ABERTO', 'FECHADO');
CREATE TYPE status_venda AS ENUM ('ABERTA', 'FINALIZADA', 'CANCELADA');
CREATE TYPE forma_pagamento AS ENUM ('DINHEIRO', 'CARTAO_DEBITO', 'CARTAO_CREDITO', 'PIX', 'FIADO');
CREATE TYPE status_pagamento AS ENUM ('PENDENTE', 'CONFIRMADO', 'ESTORNADO');
CREATE TYPE status_compra AS ENUM ('PEDIDO', 'RECEBIDO', 'CANCELADO');
CREATE TYPE tipo_movimentacao_estoque AS ENUM ('ENTRADA', 'SAIDA', 'AJUSTE', 'INVENTARIO', 'DEVOLUCAO');
CREATE TYPE tipo_movimentacao_caixa AS ENUM ('ABERTURA', 'SANGRIA', 'SUPRIMENTO', 'FECHAMENTO', 'VENDA', 'CANCELAMENTO', 'ESTORNO');
CREATE TYPE status_sync AS ENUM ('PENDENTE', 'PROCESSANDO', 'SINCRONIZADO', 'ERRO');
CREATE TYPE operacao_sync AS ENUM ('INSERT', 'UPDATE', 'DELETE');
CREATE TYPE status_conta AS ENUM ('ABERTA', 'PAGA', 'VENCIDA', 'CANCELADA');
CREATE TYPE tipo_conta AS ENUM ('PAGAR', 'RECEBER');

-- ===== TENANT =====
CREATE TABLE tenant (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome            VARCHAR(150) NOT NULL,
    cnpj            VARCHAR(18) UNIQUE,
    email           VARCHAR(150),
    telefone        VARCHAR(20),
    endereco        VARCHAR(255),
    cidade          VARCHAR(100),
    estado          VARCHAR(2),
    cep             VARCHAR(10),
    logo_url        VARCHAR(500),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    plano           VARCHAR(50) NOT NULL DEFAULT 'BASICO',
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID,
    atualizado_por  UUID
);

-- ===== USUARIO =====
CREATE TABLE usuario (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    senha_hash      VARCHAR(255) NOT NULL,
    role            role_usuario NOT NULL DEFAULT 'OPERADOR',
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    desconto_max    DOUBLE PRECISION NOT NULL DEFAULT 0.00,
    pin_hash        VARCHAR(255),
    ultimo_login    TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID,
    atualizado_por  UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, email)
);

-- ===== REFRESH TOKEN =====
CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expiracao   TIMESTAMPTZ NOT NULL,
    revogado    BOOLEAN NOT NULL DEFAULT FALSE,
    device_id   VARCHAR(100),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por  UUID,
    atualizado_por UUID
);

-- ===== PDV (dispositivo) =====
CREATE TABLE pdv (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    nome            VARCHAR(100) NOT NULL,
    descricao       VARCHAR(255),
    device_id       VARCHAR(100) UNIQUE,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID,
    atualizado_por  UUID
);

-- ===== CATEGORIA PRODUTO =====
CREATE TABLE categoria_produto (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    nome            VARCHAR(100) NOT NULL,
    pai_id          UUID REFERENCES categoria_produto(id),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID,
    atualizado_por  UUID
);

-- ===== PRODUTO =====
CREATE TABLE produto (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    categoria_id        UUID REFERENCES categoria_produto(id),
    nome                VARCHAR(200) NOT NULL,
    descricao           TEXT,
    codigo_barras       VARCHAR(50),
    sku                 VARCHAR(50),
    ncm                 VARCHAR(10),
    unidade             VARCHAR(10) NOT NULL DEFAULT 'UN',
    preco_custo         NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    preco_venda         NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    margem_lucro        NUMERIC(5,2) GENERATED ALWAYS AS (
                            CASE WHEN preco_custo > 0
                                 THEN ROUND(((preco_venda - preco_custo) / preco_custo) * 100, 2)
                                 ELSE 0
                            END
                        ) STORED,
    estoque_atual       NUMERIC(12,3) NOT NULL DEFAULT 0.000,
    estoque_minimo      NUMERIC(12,3) NOT NULL DEFAULT 0.000,
    foto_url            VARCHAR(500),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    composto            BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    atualizado_por      UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, codigo_barras),
    UNIQUE (tenant_id, sku)
);

-- ===== PRODUTO COMPOSTO (kit) =====
CREATE TABLE produto_componente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    produto_pai_id      UUID NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    produto_filho_id    UUID NOT NULL REFERENCES produto(id),
    quantidade          NUMERIC(12,3) NOT NULL,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    UNIQUE (produto_pai_id, produto_filho_id)
);

-- ===== HISTORICO PRECO PRODUTO =====
CREATE TABLE historico_preco_produto (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    produto_id      UUID NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    preco_custo     NUMERIC(12,2),
    preco_venda     NUMERIC(12,2),
    usuario_id      UUID REFERENCES usuario(id),
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID
);

-- ===== CLIENTE =====
CREATE TABLE cliente (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    nome                VARCHAR(150) NOT NULL,
    cpf_cnpj            VARCHAR(18),
    email               VARCHAR(150),
    telefone            VARCHAR(20),
    endereco            VARCHAR(255),
    cidade              VARCHAR(100),
    estado              VARCHAR(2),
    cep                 VARCHAR(10),
    data_nascimento     DATE,
    limite_credito      NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    saldo_devedor       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    tags                TEXT[],
    observacao          TEXT,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    atualizado_por      UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, cpf_cnpj)
);

-- ===== FORNECEDOR =====
CREATE TABLE fornecedor (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    razao_social        VARCHAR(200) NOT NULL,
    nome_fantasia       VARCHAR(200),
    cnpj                VARCHAR(18),
    email               VARCHAR(150),
    telefone            VARCHAR(20),
    endereco            VARCHAR(255),
    cidade              VARCHAR(100),
    estado              VARCHAR(2),
    cep                 VARCHAR(10),
    contato             VARCHAR(150),
    observacao          TEXT,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    atualizado_por      UUID,
    UNIQUE (tenant_id, cnpj)
);

-- ===== CAIXA =====
CREATE TABLE caixa (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    pdv_id              UUID NOT NULL REFERENCES pdv(id),
    operador_id         UUID NOT NULL REFERENCES usuario(id),
    dt_abertura         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dt_fechamento       TIMESTAMPTZ,
    valor_inicial       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_dinheiro      NUMERIC(12,2),
    total_debito        NUMERIC(12,2),
    total_credito       NUMERIC(12,2),
    total_pix           NUMERIC(12,2),
    total_fiado         NUMERIC(12,2),
    total_sangria       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_suprimento    NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status              status_caixa NOT NULL DEFAULT 'ABERTO',
    observacao_fechamento TEXT,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    atualizado_por      UUID
);

-- ===== MOVIMENTACAO CAIXA =====
CREATE TABLE movimentacao_caixa (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caixa_id    UUID NOT NULL REFERENCES caixa(id) ON DELETE CASCADE,
    tipo        tipo_movimentacao_caixa NOT NULL,
    valor       NUMERIC(12,2) NOT NULL,
    motivo      VARCHAR(255),
    usuario_id  UUID NOT NULL REFERENCES usuario(id),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por  UUID
);

-- ===== VENDA =====
CREATE TABLE venda (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    caixa_id            UUID NOT NULL REFERENCES caixa(id),
    pdv_id              UUID NOT NULL REFERENCES pdv(id),
    operador_id         UUID NOT NULL REFERENCES usuario(id),
    cliente_id          UUID REFERENCES cliente(id),
    numero_venda        VARCHAR(30) NOT NULL,
    dt_venda            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    subtotal            NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    desconto            NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total               NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status              status_venda NOT NULL DEFAULT 'ABERTA',
    motivo_cancelamento TEXT,
    cancelado_por       UUID REFERENCES usuario(id),
    dt_cancelamento     TIMESTAMPTZ,
    sincronizado        BOOLEAN NOT NULL DEFAULT FALSE,
    device_id           VARCHAR(100),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    atualizado_por      UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, numero_venda)
);

-- ===== ITEM VENDA =====
CREATE TABLE item_venda (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_id        UUID NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    produto_id      UUID NOT NULL REFERENCES produto(id),
    nome_produto    VARCHAR(200) NOT NULL,
    codigo_barras   VARCHAR(50),
    quantidade      NUMERIC(12,3) NOT NULL,
    preco_unitario  NUMERIC(12,2) NOT NULL,
    desconto_item   NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_item      NUMERIC(12,2) NOT NULL,
    cancelado       BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID
);

-- ===== PAGAMENTO VENDA =====
CREATE TABLE pagamento_venda (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_id    UUID NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    forma       forma_pagamento NOT NULL,
    valor       NUMERIC(12,2) NOT NULL,
    troco       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status      status_pagamento NOT NULL DEFAULT 'PENDENTE',
    nsu         VARCHAR(50),
    autorizacao VARCHAR(50),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por  UUID
);

-- ===== COMPRA =====
CREATE TABLE compra (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    fornecedor_id   UUID NOT NULL REFERENCES fornecedor(id),
    numero_nota     VARCHAR(50),
    dt_compra       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dt_entrega      TIMESTAMPTZ,
    total           NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status          status_compra NOT NULL DEFAULT 'PEDIDO',
    observacao      TEXT,
    usuario_id      UUID NOT NULL REFERENCES usuario(id),
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID,
    atualizado_por  UUID
);

-- ===== ITEM COMPRA =====
CREATE TABLE item_compra (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    compra_id       UUID NOT NULL REFERENCES compra(id) ON DELETE CASCADE,
    produto_id      UUID NOT NULL REFERENCES produto(id),
    quantidade      NUMERIC(12,3) NOT NULL,
    preco_custo     NUMERIC(12,2) NOT NULL,
    total_item      NUMERIC(12,2) NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por      UUID
);

-- ===== MOVIMENTACAO ESTOQUE =====
CREATE TABLE movimentacao_estoque (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    produto_id          UUID NOT NULL REFERENCES produto(id),
    tipo                tipo_movimentacao_estoque NOT NULL,
    quantidade          NUMERIC(12,3) NOT NULL,
    quantidade_anterior NUMERIC(12,3) NOT NULL,
    quantidade_posterior NUMERIC(12,3) NOT NULL,
    motivo              VARCHAR(255),
    referencia_id       UUID,
    referencia_tipo     VARCHAR(50),
    usuario_id          UUID REFERENCES usuario(id),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID
);

-- ===== CONTA FINANCEIRA =====
CREATE TABLE conta_financeira (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    tipo                tipo_conta NOT NULL,
    descricao           VARCHAR(255) NOT NULL,
    valor               NUMERIC(12,2) NOT NULL,
    dt_vencimento       DATE NOT NULL,
    dt_pagamento        DATE,
    status              status_conta NOT NULL DEFAULT 'ABERTA',
    referencia_id       UUID,
    referencia_tipo     VARCHAR(50),
    cliente_id          UUID REFERENCES cliente(id),
    fornecedor_id       UUID REFERENCES fornecedor(id),
    usuario_id          UUID REFERENCES usuario(id),
    observacao          TEXT,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por          UUID,
    atualizado_por      UUID
);

-- ===== FILA SINCRONIZACAO =====
CREATE TABLE fila_sincronizacao (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    pdv_id              UUID REFERENCES pdv(id),
    entidade            VARCHAR(100) NOT NULL,
    operacao            operacao_sync NOT NULL,
    payload             JSONB NOT NULL,
    status              status_sync NOT NULL DEFAULT 'PENDENTE',
    tentativas          INT NOT NULL DEFAULT 0,
    erro_mensagem       TEXT,
    device_id           VARCHAR(100),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sincronizado_em     TIMESTAMPTZ,
    criado_por          UUID
);

-- ===== AUDIT LOG =====
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID REFERENCES tenant(id),
    usuario_id  UUID REFERENCES usuario(id),
    acao        VARCHAR(100) NOT NULL,
    entidade    VARCHAR(100) NOT NULL,
    entidade_id VARCHAR(100),
    payload_antes JSONB,
    payload_depois JSONB,
    ip          VARCHAR(50),
    user_agent  TEXT,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    criado_por  UUID
);

-- ============================================================
-- ÍNDICES
-- ============================================================

CREATE INDEX idx_tenant_ativo ON tenant(ativo);
CREATE INDEX idx_usuario_tenant ON usuario(tenant_id);
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_produto_tenant ON produto(tenant_id);
CREATE INDEX idx_produto_categoria ON produto(categoria_id);
CREATE INDEX idx_produto_codigo_barras ON produto(tenant_id, codigo_barras);
CREATE INDEX idx_produto_ativo ON produto(tenant_id, ativo);
CREATE INDEX idx_produto_estoque_minimo ON produto(tenant_id) WHERE estoque_atual <= estoque_minimo;
CREATE INDEX idx_caixa_tenant ON caixa(tenant_id);
CREATE INDEX idx_caixa_pdv ON caixa(pdv_id);
CREATE INDEX idx_caixa_status ON caixa(tenant_id, status);
CREATE INDEX idx_venda_tenant ON venda(tenant_id);
CREATE INDEX idx_venda_caixa ON venda(caixa_id);
CREATE INDEX idx_venda_cliente ON venda(cliente_id);
CREATE INDEX idx_venda_dt_venda ON venda(tenant_id, dt_venda);
CREATE INDEX idx_venda_status ON venda(tenant_id, status);
CREATE INDEX idx_venda_sincronizado ON venda(sincronizado) WHERE sincronizado = FALSE;
CREATE INDEX idx_venda_numero ON venda(tenant_id, numero_venda);
CREATE INDEX idx_item_venda_venda ON item_venda(venda_id);
CREATE INDEX idx_item_venda_produto ON item_venda(produto_id);
CREATE INDEX idx_pagamento_venda ON pagamento_venda(venda_id);
CREATE INDEX idx_mov_estoque_tenant ON movimentacao_estoque(tenant_id);
CREATE INDEX idx_mov_estoque_produto ON movimentacao_estoque(produto_id);
CREATE INDEX idx_mov_estoque_data ON movimentacao_estoque(tenant_id, criado_em);
CREATE INDEX idx_cliente_tenant ON cliente(tenant_id);
CREATE INDEX idx_cliente_cpf_cnpj ON cliente(tenant_id, cpf_cnpj);
CREATE INDEX idx_fornecedor_tenant ON fornecedor(tenant_id);
CREATE INDEX idx_compra_tenant ON compra(tenant_id);
CREATE INDEX idx_compra_fornecedor ON compra(fornecedor_id);
CREATE INDEX idx_conta_tenant ON conta_financeira(tenant_id);
CREATE INDEX idx_conta_tipo ON conta_financeira(tenant_id, tipo);
CREATE INDEX idx_conta_status ON conta_financeira(tenant_id, status);
CREATE INDEX idx_conta_vencimento ON conta_financeira(tenant_id, dt_vencimento);
CREATE INDEX idx_sync_tenant ON fila_sincronizacao(tenant_id);
CREATE INDEX idx_sync_status ON fila_sincronizacao(status);
CREATE INDEX idx_sync_device ON fila_sincronizacao(device_id);
CREATE INDEX idx_audit_tenant ON audit_log(tenant_id);
CREATE INDEX idx_audit_usuario ON audit_log(usuario_id);
CREATE INDEX idx_audit_entidade ON audit_log(entidade, entidade_id);
CREATE INDEX idx_audit_data ON audit_log(criado_em);

-- ============================================================
-- SEED (dados iniciais para desenvolvimento)
-- ============================================================

INSERT INTO tenant (id, nome, cnpj, email, plano)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Empresa Demo',
    '00.000.000/0001-00',
    'demo@pdvsaas.com',
    'PREMIUM'
);

INSERT INTO usuario (id, tenant_id, nome, email, senha_hash, role)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'Administrador',
    'admin@pdvsaas.com',
    '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
    'ADMIN'
);

INSERT INTO pdv (id, tenant_id, nome, descricao, device_id)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'Caixa 01',
    'Caixa principal',
    'DEVICE-00001'
);