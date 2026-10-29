package com.pdv.modules.audit.repository;

import com.pdv.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
            AND (:usuarioId IS NULL OR a.usuarioId = :usuarioId)
            AND (:entidade IS NULL OR a.entidade = :entidade)
            AND (:entidadeId IS NULL OR a.entidadeId = :entidadeId)
            AND (:inicio IS NULL OR a.criadoEm >= :inicio)
            AND (:fim IS NULL OR a.criadoEm <= :fim)
            ORDER BY a.criadoEm DESC
            """)
    Page<AuditLog> findByFiltros(@Param("tenantId") UUID tenantId,
                                  @Param("usuarioId") UUID usuarioId,
                                  @Param("entidade") String entidade,
                                  @Param("entidadeId") String entidadeId,
                                  @Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  Pageable pageable);
}
