package com.pdv.modules.tenant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pdv.modules.tenant.entity.Tenant;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    Page<Tenant> findByAtivoTrue(Pageable pageable);

    @Query("""
            SELECT t FROM Tenant t
            WHERE (CAST(:nome AS string) IS NULL
                   OR LOWER(t.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
            AND (:ativo IS NULL OR t.ativo = :ativo)
            """)
    Page<Tenant> findByFiltros(@Param("nome") String nome,
                               @Param("ativo") Boolean ativo,
                               Pageable pageable);
    
    /** Retorna todos os tenants ativos para o job de alertas. */
    @Query("SELECT t FROM Tenant t WHERE t.ativo = true")
    List<Tenant> findAllAtivos();
}