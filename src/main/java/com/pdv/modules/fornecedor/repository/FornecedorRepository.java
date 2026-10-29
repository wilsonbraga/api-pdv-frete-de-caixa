package com.pdv.modules.fornecedor.repository;

import com.pdv.modules.fornecedor.entity.Fornecedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {

    Optional<Fornecedor> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByCnpjAndTenantId(String cnpj, UUID tenantId);

    /**
     * FIX: O erro "function lower(bytea) does not exist" ocorre porque o Hibernate
     * envia parâmetros nulos como bytea para o PostgreSQL.
     * Solução: usar CAST(:nome AS string) para forçar o tipo varchar,
     * permitindo que o PostgreSQL aplique LOWER() corretamente.
     */
    @Query("""
            SELECT f FROM Fornecedor f
            WHERE f.tenant.id = :tenantId
            AND (CAST(:nome AS string) IS NULL
                 OR LOWER(f.razaoSocial) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%'))
                 OR LOWER(f.nomeFantasia) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
            AND (:ativo IS NULL OR f.ativo = :ativo)
            """)
    Page<Fornecedor> findByFiltros(@Param("tenantId") UUID tenantId,
                                   @Param("nome") String nome,
                                   @Param("ativo") Boolean ativo,
                                   Pageable pageable);
}