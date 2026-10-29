package com.pdv.modules.fornecedor.repository;

import com.pdv.modules.fornecedor.entity.Compra;
import com.pdv.modules.fornecedor.entity.StatusCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {

    Optional<Compra> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT c FROM Compra c
            LEFT JOIN FETCH c.itens i
            LEFT JOIN FETCH i.produto p
            LEFT JOIN FETCH c.fornecedor f
            WHERE c.id = :id AND c.tenant.id = :tenantId
            """)
    Optional<Compra> findByIdComDetalhes(@Param("id") UUID id,
                                          @Param("tenantId") UUID tenantId);

    @Query("""
            SELECT c FROM Compra c
            WHERE c.tenant.id = :tenantId
            AND (:fornecedorId IS NULL OR c.fornecedor.id = :fornecedorId)
            AND (:status IS NULL OR c.status = :status)
            AND (:inicio IS NULL OR c.dtCompra >= :inicio)
            AND (:fim IS NULL OR c.dtCompra <= :fim)
            ORDER BY c.dtCompra DESC
            """)
    Page<Compra> findByFiltros(@Param("tenantId") UUID tenantId,
                                @Param("fornecedorId") UUID fornecedorId,
                                @Param("status") StatusCompra status,
                                @Param("inicio") LocalDateTime inicio,
                                @Param("fim") LocalDateTime fim,
                                Pageable pageable);
}
