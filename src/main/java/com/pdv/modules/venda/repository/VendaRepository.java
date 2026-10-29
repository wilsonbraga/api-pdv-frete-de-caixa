package com.pdv.modules.venda.repository;

import com.pdv.modules.venda.entity.StatusVenda;
import com.pdv.modules.venda.entity.Venda;
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
public interface VendaRepository extends JpaRepository<Venda, UUID> {

    Optional<Venda> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Venda> findByNumeroVendaAndTenantId(String numeroVenda, UUID tenantId);

    boolean existsByNumeroVendaAndTenantId(String numeroVenda, UUID tenantId);

    @Query("""
            SELECT v FROM Venda v
            LEFT JOIN FETCH v.itens i
            LEFT JOIN FETCH v.cliente c
            LEFT JOIN FETCH v.operador o
            WHERE v.id = :id AND v.tenant.id = :tenantId
            ORDER BY i.criadoEm ASC
            """)
    Optional<Venda> findByIdComItens(@Param("id") UUID id,
                                      @Param("tenantId") UUID tenantId);

    @Query("""
            SELECT v FROM Venda v
            LEFT JOIN FETCH v.pagamentos p
            WHERE v.id = :id AND v.tenant.id = :tenantId
            """)
    Optional<Venda> findByIdComPagamentos(@Param("id") UUID id,
                                           @Param("tenantId") UUID tenantId);

    @Query("""
            SELECT v FROM Venda v
            WHERE v.tenant.id = :tenantId
            AND (:caixaId IS NULL OR v.caixa.id = :caixaId)
            AND (:pdvId IS NULL OR v.pdv.id = :pdvId)
            AND (:operadorId IS NULL OR v.operador.id = :operadorId)
            AND (:clienteId IS NULL OR v.cliente.id = :clienteId)
            AND (:status IS NULL OR v.status = :status)
            AND (:inicio IS NULL OR v.dtVenda >= :inicio)
            AND (:fim IS NULL OR v.dtVenda <= :fim)
            ORDER BY v.dtVenda DESC
            """)
    Page<Venda> findByFiltros(@Param("tenantId") UUID tenantId,
                               @Param("caixaId") UUID caixaId,
                               @Param("pdvId") UUID pdvId,
                               @Param("operadorId") UUID operadorId,
                               @Param("clienteId") UUID clienteId,
                               @Param("status") StatusVenda status,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim,
                               Pageable pageable);

    @Query("""
            SELECT COALESCE(MAX(CAST(SUBSTRING(v.numeroVenda, LENGTH(:prefixo) + 1) AS int)), 0)
            FROM Venda v
            WHERE v.tenant.id = :tenantId
            AND v.numeroVenda LIKE CONCAT(:prefixo, '%')
            """)
    int findMaxSequencial(@Param("tenantId") UUID tenantId, @Param("prefixo") String prefixo);

    @Query("""
            SELECT COUNT(v) FROM Venda v
            WHERE v.tenant.id = :tenantId
            AND v.status = 'FINALIZADA'
            AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
            """)
    long countVendasFinalizadasPeriodo(@Param("tenantId") UUID tenantId,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT COALESCE(SUM(v.total), 0) FROM Venda v
            WHERE v.tenant.id = :tenantId
            AND v.status = 'FINALIZADA'
            AND v.dtVenda >= :inicio AND v.dtVenda <= :fim
            """)
    java.math.BigDecimal sumTotalVendasPeriodo(@Param("tenantId") UUID tenantId,
                                                @Param("inicio") LocalDateTime inicio,
                                                @Param("fim") LocalDateTime fim);
}