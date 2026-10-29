package com.pdv.modules.caixa.repository;

import com.pdv.modules.caixa.entity.Caixa;
import com.pdv.modules.caixa.entity.StatusCaixa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaixaRepository extends JpaRepository<Caixa, UUID> {

    Optional<Caixa> findByIdAndTenantId(UUID id, UUID tenantId);

    // Verifica se já existe caixa aberto para o PDV
    Optional<Caixa> findByPdvIdAndStatus(UUID pdvId, StatusCaixa status);

    // Caixa aberto pelo operador logado
    Optional<Caixa> findByOperadorIdAndStatus(UUID operadorId, StatusCaixa status);

    // Lista caixas abertos de todos os PDVs do tenant (para visão do gerente)
    @Query("""
            SELECT c FROM Caixa c
            JOIN FETCH c.pdv p
            JOIN FETCH c.operador o
            WHERE c.tenant.id = :tenantId
            AND c.status = 'ABERTO'
            """)
    List<Caixa> findCaixasAbertos(@Param("tenantId") UUID tenantId);

    @Query("""
            SELECT c FROM Caixa c
            JOIN FETCH c.pdv p
            JOIN FETCH c.operador o
            WHERE c.tenant.id = :tenantId
            AND (:pdvId IS NULL OR c.pdv.id = :pdvId)
            AND (:operadorId IS NULL OR c.operador.id = :operadorId)
            AND (:status IS NULL OR c.status = :status)
            AND (:inicio IS NULL OR c.dtAbertura >= :inicio)
            AND (:fim IS NULL OR c.dtAbertura <= :fim)
            ORDER BY c.dtAbertura DESC
            """)
    Page<Caixa> findByFiltros(@Param("tenantId") UUID tenantId,
                               @Param("pdvId") UUID pdvId,
                               @Param("operadorId") UUID operadorId,
                               @Param("status") StatusCaixa status,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fim") LocalDateTime fim,
                               Pageable pageable);
    
 // Verifica se já existiu caixa (aberto OU fechado) para este PDV hoje
    @Query("""
            SELECT COUNT(c) > 0 FROM Caixa c
            WHERE c.pdv.id = :pdvId
            AND c.tenant.id = :tenantId
            AND c.dtAbertura >= :inicioDia
            AND c.dtAbertura < :fimDia
            """)
    boolean existeCaixaNoDia(@Param("pdvId") UUID pdvId,
                              @Param("tenantId") UUID tenantId,
                              @Param("inicioDia") LocalDateTime inicioDia,
                              @Param("fimDia") LocalDateTime fimDia);
}
