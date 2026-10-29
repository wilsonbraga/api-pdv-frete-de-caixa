-- ============================================================
-- V2__seed_dados_iniciais.sql
-- PDV Offline SaaS - Dados iniciais reais para desenvolvimento
--
-- Senha padrão de todos os usuários: Admin@123
-- Hash BCrypt gerado com strength 12
-- ============================================================

-- ============================================================
-- TENANT 1 — Mercadinho São João (varejo alimentar)
-- ============================================================
INSERT INTO tenant (id, nome, cnpj, email, telefone, endereco, cidade, estado, cep, plano)
VALUES (
    'aaaaaaaa-0000-0000-0000-000000000001',
    'Mercadinho São João',
    '12.345.678/0001-90',
    'contato@mercadinhosojoao.com.br',
    '(11) 99999-1111',
    'Rua das Flores, 100',
    'São Paulo',
    'SP',
    '01310-100',
    'PREMIUM'
);

-- ============================================================
-- TENANT 2 — Farmácia Vida & Saúde
-- ============================================================
INSERT INTO tenant (id, nome, cnpj, email, telefone, endereco, cidade, estado, cep, plano)
VALUES (
    'bbbbbbbb-0000-0000-0000-000000000001',
    'Farmácia Vida & Saúde',
    '98.765.432/0001-10',
    'contato@farmaciavida.com.br',
    '(21) 98888-2222',
    'Av. Principal, 500',
    'Rio de Janeiro',
    'RJ',
    '20040-020',
    'BASICO'
);

-- ============================================================
-- USUÁRIOS — Tenant 1 (Mercadinho)
-- Todos com senha: Admin@123
-- ============================================================
INSERT INTO usuario (id, tenant_id, nome, email, senha_hash, role, desconto_max)
VALUES
    -- Admin
    ('aaaaaaaa-0001-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'Wilson Braga',
     'wilson@mercadinhosojoao.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'ADMIN', 100.00),

    -- Gerente
    ('aaaaaaaa-0002-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'Carlos Mendes',
     'carlos@mercadinhosojoao.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'GERENTE', 20.00),

    -- Operador 1
    ('aaaaaaaa-0003-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'Ana Paula Silva',
     'ana@mercadinhosojoao.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'OPERADOR', 5.00),

    -- Operador 2
    ('aaaaaaaa-0004-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'João Ferreira',
     'joao@mercadinhosojoao.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'OPERADOR', 5.00),

    -- Financeiro
    ('aaaaaaaa-0005-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'Maria Contadora',
     'financeiro@mercadinhosojoao.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'FINANCEIRO', 0.00);

-- ============================================================
-- USUÁRIOS — Tenant 2 (Farmácia)
-- ============================================================
INSERT INTO usuario (id, tenant_id, nome, email, senha_hash, role, desconto_max)
VALUES
    ('bbbbbbbb-0001-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001',
     'Roberta Farmacêutica',
     'roberta@farmaciavida.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'ADMIN', 100.00),

    ('bbbbbbbb-0002-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001',
     'Pedro Atendente',
     'pedro@farmaciavida.com.br',
     '$2a$12$9PIBlucYl8rtumtD0zvBb.aYbvyKh3jvOlPm83ZYK5Wl2Cz7BHtI2',
     'OPERADOR', 5.00);

-- ============================================================
-- PDVs — Tenant 1 (Mercadinho)
-- ============================================================
INSERT INTO pdv (id, tenant_id, nome, descricao, device_id)
VALUES
    ('aaaaaaaa-0000-0001-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'Caixa 01', 'Caixa principal - entrada', 'DEVICE-MERC-001'),

    ('aaaaaaaa-0000-0002-0000-000000000001',
     'aaaaaaaa-0000-0000-0000-000000000001',
     'Caixa 02', 'Caixa secundário - fundo', 'DEVICE-MERC-002');

-- ============================================================
-- PDVs — Tenant 2 (Farmácia)
-- ============================================================
INSERT INTO pdv (id, tenant_id, nome, descricao, device_id)
VALUES
    ('bbbbbbbb-0000-0001-0000-000000000001',
     'bbbbbbbb-0000-0000-0000-000000000001',
     'Caixa Único', 'Caixa da farmácia', 'DEVICE-FARM-001');

-- ============================================================
-- CATEGORIAS — Tenant 1 (Mercadinho)
-- ============================================================
INSERT INTO categoria_produto (id, tenant_id, nome, pai_id)
VALUES
    -- Raízes
    ('aaaaaaaa-0000-0000-0001-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Alimentos', NULL),
    ('aaaaaaaa-0000-0000-0002-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Bebidas', NULL),
    ('aaaaaaaa-0000-0000-0003-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Limpeza', NULL),
    ('aaaaaaaa-0000-0000-0004-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Higiene Pessoal', NULL),
    ('aaaaaaaa-0000-0000-0005-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Frios e Laticínios', NULL),
    -- Filhas de Alimentos
    ('aaaaaaaa-0000-0000-0011-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Grãos e Cereais',
     'aaaaaaaa-0000-0000-0001-000000000001'),
    ('aaaaaaaa-0000-0000-0012-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Massas e Farinhas',
     'aaaaaaaa-0000-0000-0001-000000000001'),
    ('aaaaaaaa-0000-0000-0013-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Conservas e Enlatados',
     'aaaaaaaa-0000-0000-0001-000000000001'),
    -- Filhas de Bebidas
    ('aaaaaaaa-0000-0000-0021-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Refrigerantes',
     'aaaaaaaa-0000-0000-0002-000000000001'),
    ('aaaaaaaa-0000-0000-0022-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Sucos e Néctares',
     'aaaaaaaa-0000-0000-0002-000000000001'),
    ('aaaaaaaa-0000-0000-0023-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Águas',
     'aaaaaaaa-0000-0000-0002-000000000001');

-- ============================================================
-- CATEGORIAS — Tenant 2 (Farmácia)
-- ============================================================
INSERT INTO categoria_produto (id, tenant_id, nome, pai_id)
VALUES
    ('bbbbbbbb-0000-0000-0001-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001', 'Medicamentos', NULL),
    ('bbbbbbbb-0000-0000-0002-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001', 'Dermocosméticos', NULL),
    ('bbbbbbbb-0000-0000-0003-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001', 'Vitaminas e Suplementos', NULL);

-- ============================================================
-- PRODUTOS — Tenant 1 (Mercadinho) - 20 produtos
-- ============================================================
INSERT INTO produto (id, tenant_id, categoria_id, nome, codigo_barras, sku, unidade,
                     preco_custo, preco_venda, estoque_atual, estoque_minimo)
VALUES
    -- Grãos
    ('aaaaaaaa-0000-0000-0000-000000000101', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0011-000000000001',
     'Arroz Branco Tipo 1 5kg', '7891234560001', 'ARR-001', 'UN', 12.50, 22.90, 50.000, 10.000),

    ('aaaaaaaa-0000-0000-0000-000000000102', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0011-000000000001',
     'Feijão Carioca 1kg', '7891234560002', 'FEI-001', 'UN', 4.80, 8.99, 80.000, 20.000),

    ('aaaaaaaa-0000-0000-0000-000000000103', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0011-000000000001',
     'Açúcar Cristal 1kg', '7891234560003', 'ACU-001', 'UN', 2.90, 5.49, 60.000, 15.000),

    ('aaaaaaaa-0000-0000-0000-000000000104', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0011-000000000001',
     'Sal Refinado 1kg', '7891234560004', 'SAL-001', 'UN', 1.20, 2.99, 40.000, 10.000),

    -- Massas
    ('aaaaaaaa-0000-0000-0000-000000000105', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0012-000000000001',
     'Macarrão Espaguete 500g', '7891234560005', 'MAC-001', 'UN', 2.50, 4.99, 35.000, 10.000),

    ('aaaaaaaa-0000-0000-0000-000000000106', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0012-000000000001',
     'Farinha de Trigo 1kg', '7891234560006', 'FAR-001', 'UN', 3.10, 5.99, 45.000, 10.000),

    -- Conservas
    ('aaaaaaaa-0000-0000-0000-000000000107', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0013-000000000001',
     'Óleo de Soja 900ml', '7891234560007', 'OLE-001', 'UN', 5.60, 9.99, 30.000, 8.000),

    ('aaaaaaaa-0000-0000-0000-000000000108', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0013-000000000001',
     'Extrato de Tomate 340g', '7891234560008', 'EXT-001', 'UN', 1.80, 3.49, 55.000, 12.000),

    ('aaaaaaaa-0000-0000-0000-000000000109', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0013-000000000001',
     'Atum em Lata 170g', '7891234560009', 'ATU-001', 'UN', 4.20, 7.99, 40.000, 10.000),

    -- Refrigerantes
    ('aaaaaaaa-0000-0000-0000-000000000110', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0021-000000000001',
     'Coca-Cola 2L', '7891234560010', 'COC-001', 'UN', 5.50, 9.99, 48.000, 12.000),

    ('aaaaaaaa-0000-0000-0000-000000000111', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0021-000000000001',
     'Guaraná Antarctica 2L', '7891234560011', 'GUA-001', 'UN', 4.80, 8.99, 36.000, 12.000),

    ('aaaaaaaa-0000-0000-0000-000000000112', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0021-000000000001',
     'Refrigerante Lata 350ml', '7891234560012', 'REF-001', 'UN', 2.20, 4.49, 120.000, 24.000),

    -- Sucos
    ('aaaaaaaa-0000-0000-0000-000000000113', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0022-000000000001',
     'Suco de Laranja 1L', '7891234560013', 'SUC-001', 'UN', 4.50, 8.49, 25.000, 6.000),

    -- Água
    ('aaaaaaaa-0000-0000-0000-000000000114', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0023-000000000001',
     'Água Mineral 500ml', '7891234560014', 'AGU-001', 'UN', 0.90, 2.49, 96.000, 24.000),

    ('aaaaaaaa-0000-0000-0000-000000000115', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0023-000000000001',
     'Água Mineral 1,5L', '7891234560015', 'AGU-002', 'UN', 1.50, 3.99, 60.000, 12.000),

    -- Limpeza
    ('aaaaaaaa-0000-0000-0000-000000000116', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0003-000000000001',
     'Detergente Líquido 500ml', '7891234560016', 'DET-001', 'UN', 1.40, 2.99, 50.000, 10.000),

    ('aaaaaaaa-0000-0000-0000-000000000117', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0003-000000000001',
     'Sabão em Pó 1kg', '7891234560017', 'SAB-001', 'UN', 5.90, 10.99, 30.000, 8.000),

    -- Higiene
    ('aaaaaaaa-0000-0000-0000-000000000118', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0004-000000000001',
     'Papel Higiênico 4 rolos', '7891234560018', 'PAP-001', 'PCT', 4.20, 7.99, 40.000, 10.000),

    ('aaaaaaaa-0000-0000-0000-000000000119', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0004-000000000001',
     'Shampoo 400ml', '7891234560019', 'SHA-001', 'UN', 6.80, 13.99, 20.000, 5.000),

    -- Frios
    ('aaaaaaaa-0000-0000-0000-000000000120', 'aaaaaaaa-0000-0000-0000-000000000001',
     'aaaaaaaa-0000-0000-0005-000000000001',
     'Leite Integral 1L', '7891234560020', 'LEI-001', 'UN', 3.20, 5.99, 72.000, 24.000);

-- ============================================================
-- PRODUTOS — Tenant 2 (Farmácia)
-- ============================================================
INSERT INTO produto (id, tenant_id, categoria_id, nome, codigo_barras, sku, unidade,
                     preco_custo, preco_venda, estoque_atual, estoque_minimo)
VALUES
    ('bbbbbbbb-0000-0000-0000-000000000101', 'bbbbbbbb-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0001-000000000001',
     'Dipirona 500mg 20 comp', '7899876540001', 'DIP-001', 'CX', 4.50, 9.90, 30.000, 10.000),

    ('bbbbbbbb-0000-0000-0000-000000000102', 'bbbbbbbb-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0001-000000000001',
     'Paracetamol 750mg 20 comp', '7899876540002', 'PAR-001', 'CX', 3.80, 8.90, 25.000, 8.000),

    ('bbbbbbbb-0000-0000-0000-000000000103', 'bbbbbbbb-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0002-000000000001',
     'Protetor Solar FPS 50 120ml', '7899876540003', 'PRO-001', 'UN', 18.00, 39.90, 15.000, 5.000),

    ('bbbbbbbb-0000-0000-0000-000000000104', 'bbbbbbbb-0000-0000-0000-000000000001',
     'bbbbbbbb-0000-0000-0003-000000000001',
     'Vitamina C 1g Efervescente 10 comp', '7899876540004', 'VIT-001', 'CX', 8.90, 18.90, 20.000, 6.000);

-- ============================================================
-- CLIENTES — Tenant 1 (Mercadinho)
-- ============================================================
INSERT INTO cliente (id, tenant_id, nome, cpf_cnpj, telefone, cidade, estado,
                     limite_credito, saldo_devedor)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000201', 'aaaaaaaa-0000-0000-0000-000000000001',
     'José da Silva Santos', '123.456.789-00', '(11) 98765-4321',
     'São Paulo', 'SP', 500.00, 0.00),

    ('aaaaaaaa-0000-0000-0000-000000000202', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Maria Oliveira Costa', '234.567.890-11', '(11) 97654-3210',
     'São Paulo', 'SP', 300.00, 45.50),

    ('aaaaaaaa-0000-0000-0000-000000000203', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Pedro Alves Rodrigues', '345.678.901-22', '(11) 96543-2109',
     'São Paulo', 'SP', 200.00, 0.00),

    ('aaaaaaaa-0000-0000-0000-000000000204', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Ana Ferreira Lima', '456.789.012-33', '(11) 95432-1098',
     'São Paulo', 'SP', 1000.00, 120.00),

    ('aaaaaaaa-0000-0000-0000-000000000205', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Carlos Eduardo Mendes', '567.890.123-44', '(11) 94321-0987',
     'São Paulo', 'SP', 150.00, 0.00);

-- ============================================================
-- FORNECEDORES — Tenant 1 (Mercadinho)
-- ============================================================
INSERT INTO fornecedor (id, tenant_id, razao_social, nome_fantasia, cnpj,
                        email, telefone, cidade, estado, contato)
VALUES
    ('aaaaaaaa-0000-0000-0000-000000000301', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Distribuidora Norte Alimentos Ltda', 'Norte Alimentos',
     '11.222.333/0001-44', 'vendas@nortealimentos.com.br',
     '(11) 3333-1111', 'São Paulo', 'SP', 'Roberto Vendas'),

    ('aaaaaaaa-0000-0000-0000-000000000302', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Bebidas Sul Distribuidora S/A', 'Bebidas Sul',
     '22.333.444/0001-55', 'comercial@bebidasul.com.br',
     '(11) 3333-2222', 'São Paulo', 'SP', 'Fernanda Comercial'),

    ('aaaaaaaa-0000-0000-0000-000000000303', 'aaaaaaaa-0000-0000-0000-000000000001',
     'Limpeza Total Distribuidora ME', 'Limpeza Total',
     '33.444.555/0001-66', 'atendimento@limpezatotal.com.br',
     '(11) 3333-3333', 'São Paulo', 'SP', 'Paulo Atendimento');

-- ============================================================
-- HISTÓRICO DE PREÇO — Produtos do Mercadinho
-- ============================================================
INSERT INTO historico_preco_produto (produto_id, preco_custo, preco_venda,
                                     usuario_id)
SELECT id, preco_custo, preco_venda,
       'aaaaaaaa-0001-0000-0000-000000000001'
FROM produto
WHERE tenant_id = 'aaaaaaaa-0000-0000-0000-000000000001';

-- ============================================================
-- HISTÓRICO DE PREÇO — Produtos da Farmácia
-- ============================================================
INSERT INTO historico_preco_produto (produto_id, preco_custo, preco_venda,
                                     usuario_id)
SELECT id, preco_custo, preco_venda,
       'bbbbbbbb-0001-0000-0000-000000000001'
FROM produto
WHERE tenant_id = 'bbbbbbbb-0000-0000-0000-000000000001';

-- ============================================================
-- RESUMO DOS DADOS INSERIDOS
-- ============================================================
-- Tenants    : 2 (Mercadinho São João + Farmácia Vida & Saúde)
-- Usuários   : 7 total (5 no mercadinho + 2 na farmácia)
-- PDVs       : 3 total (2 no mercadinho + 1 na farmácia)
-- Categorias : 14 total (11 mercadinho + 3 farmácia)
-- Produtos   : 24 total (20 mercadinho + 4 farmácia)
-- Clientes   : 5 (mercadinho)
-- Fornecedores: 3 (mercadinho)
--
-- CREDENCIAIS DE ACESSO (todos com senha Admin@123):
--   Mercadinho Admin   : wilson@mercadinhosojoao.com.br
--   Mercadinho Gerente : carlos@mercadinhosojoao.com.br
--   Mercadinho Operador: ana@mercadinhosojoao.com.br
--   Farmácia Admin     : roberta@farmaciavida.com.br
-- ============================================================
