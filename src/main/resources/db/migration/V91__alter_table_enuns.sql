-- =========================================================================
-- 1. REMOVER CONSTRAINTS E ÍNDICES QUE DEPENDEM DOS ENUMS ANTIGOS
-- =========================================================================

DO $$
DECLARE
    r RECORD;
BEGIN
    -- Dropar CHECK constraints de configuracao_pix que usam a coluna 'provedor'
    FOR r IN 
        SELECT constraint_name 
        FROM information_schema.constraint_column_usage 
        WHERE table_name = 'configuracao_pix' AND column_name = 'provedor'
    LOOP
        EXECUTE 'ALTER TABLE configuracao_pix DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name) || ' CASCADE';
    END LOOP;

    -- Dropar CHECK constraints de cobranca_pix que usam a coluna 'status'
    FOR r IN 
        SELECT constraint_name 
        FROM information_schema.constraint_column_usage 
        WHERE table_name = 'cobranca_pix' AND column_name = 'status'
    LOOP
        EXECUTE 'ALTER TABLE cobranca_pix DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name) || ' CASCADE';
    END LOOP;

    -- Dropar índices em configuracao_pix que mencionam a coluna 'provedor'
    FOR r IN 
        SELECT c.relname AS index_name
        FROM pg_index i
        JOIN pg_class c ON c.oid = i.indexrelid
        JOIN pg_class t ON t.oid = i.indrelid
        WHERE t.relname = 'configuracao_pix' 
          AND pg_get_indexdef(i.indexrelid) ILIKE '%provedor%'
    LOOP
        EXECUTE 'DROP INDEX IF EXISTS ' || quote_ident(r.index_name) || ' CASCADE';
    END LOOP;

    -- Dropar índices em cobranca_pix que mencionam a coluna 'status' (inclusive parciais)
    FOR r IN 
        SELECT c.relname AS index_name
        FROM pg_index i
        JOIN pg_class c ON c.oid = i.indexrelid
        JOIN pg_class t ON t.oid = i.indrelid
        WHERE t.relname = 'cobranca_pix' 
          AND pg_get_indexdef(i.indexrelid) ILIKE '%status%'
    LOOP
        EXECUTE 'DROP INDEX IF EXISTS ' || quote_ident(r.index_name) || ' CASCADE';
    END LOOP;
END $$;

-- =========================================================================
-- 2. REMOVER OS VALORES PADRÃO (DEFAULT)
-- =========================================================================
ALTER TABLE configuracao_pix ALTER COLUMN provedor DROP DEFAULT;
ALTER TABLE cobranca_pix ALTER COLUMN status DROP DEFAULT;

-- =========================================================================
-- 3. ALTERAR OS TIPOS DAS COLUNAS PARA VARCHAR
-- =========================================================================
ALTER TABLE configuracao_pix ALTER COLUMN provedor TYPE varchar(20) USING provedor::varchar;
ALTER TABLE cobranca_pix ALTER COLUMN status TYPE varchar(20) USING status::varchar;

-- =========================================================================
-- 4. REMOVER OS TIPOS ENUMS DO POSTGRES
-- =========================================================================
DROP TYPE IF EXISTS provedor_pix;
DROP TYPE IF EXISTS status_cobranca_pix;

-- =========================================================================
-- 5. RE-ADICIONAR OS VALORES PADRÃO COMO VARCHAR (STRINGS)
-- =========================================================================
ALTER TABLE configuracao_pix ALTER COLUMN provedor SET DEFAULT 'MERCADOPAGO';
ALTER TABLE cobranca_pix ALTER COLUMN status SET DEFAULT 'PENDENTE';