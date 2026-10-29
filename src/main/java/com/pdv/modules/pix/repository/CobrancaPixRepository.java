package com.pdv.modules.pix.repository;

import com.pdv.modules.pix.entity.CobrancaPix;
import com.pdv.modules.pix.entity.StatusCobrancaPix;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CobrancaPixRepository extends JpaRepository<CobrancaPix, UUID> {

    Optional<CobrancaPix> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<CobrancaPix> findByMpPaymentId(String mpPaymentId);

    Optional<CobrancaPix> findByVendaIdAndStatus(UUID vendaId, StatusCobrancaPix status);

    List<CobrancaPix> findByVendaId(UUID vendaId);

    // ── Listagem com filtros e paginação (tela de histórico) ──────────────
    @Query("""
            SELECT c FROM CobrancaPix c
            WHERE c.tenant.id = :tenantId
              AND (:status IS NULL OR c.status = :status)
              AND (:dataInicio IS NULL OR c.criadoEm >= :dataInicio)
              AND (:dataFim    IS NULL OR c.criadoEm <= :dataFim)
            ORDER BY c.criadoEm DESC
            """)
    Page<CobrancaPix> listarPorFiltros(
            @Param("tenantId")    UUID tenantId,
            @Param("status")      StatusCobrancaPix status,
            @Param("dataInicio")  LocalDateTime dataInicio,
            @Param("dataFim")     LocalDateTime dataFim,
            Pageable pageable
    );

    // ── Jobs de expiração ─────────────────────────────────────────────────
    @Query("""
            SELECT c FROM CobrancaPix c
            WHERE c.status = 'PENDENTE'
            AND c.expiracao < :agora
            """)
    List<CobrancaPix> findPendentesExpiradas(@Param("agora") LocalDateTime agora);

    @Modifying
    @Query("""
            UPDATE CobrancaPix c
            SET c.status = 'EXPIRADO', c.atualizadoEm = :agora
            WHERE c.status = 'PENDENTE'
            AND c.expiracao < :agora
            """)
    int expirarCobrancasVencidas(@Param("agora") LocalDateTime agora);
}
