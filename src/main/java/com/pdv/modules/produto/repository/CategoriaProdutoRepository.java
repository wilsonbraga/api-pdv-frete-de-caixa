package com.pdv.modules.produto.repository;

import com.pdv.modules.produto.entity.CategoriaProduto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoriaProdutoRepository extends JpaRepository<CategoriaProduto, UUID> {

    Optional<CategoriaProduto> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CategoriaProduto> findByTenantIdAndAtivoTrueOrderByNome(UUID tenantId);

    @Query("""
            SELECT c FROM CategoriaProduto c
            WHERE c.tenant.id = :tenantId
            AND c.pai IS NULL
            AND c.ativo = true
            ORDER BY c.nome
            """)
    List<CategoriaProduto> findRaizesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("""
            SELECT c FROM CategoriaProduto c
            WHERE c.tenant.id = :tenantId
            AND c.pai.id = :paiId
            AND c.ativo = true
            ORDER BY c.nome
            """)
    List<CategoriaProduto> findFilhasByPaiId(@Param("tenantId") UUID tenantId,
                                              @Param("paiId") UUID paiId);

    boolean existsByNomeAndTenantIdAndPaiId(String nome, UUID tenantId, UUID paiId);
}
