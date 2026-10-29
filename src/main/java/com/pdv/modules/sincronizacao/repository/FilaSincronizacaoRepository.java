package com.pdv.modules.sincronizacao.repository;

import com.pdv.modules.sincronizacao.entity.FilaSincronizacao;
import com.pdv.modules.sincronizacao.entity.StatusSync;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FilaSincronizacaoRepository extends JpaRepository<FilaSincronizacao, UUID> {

    // Busca pendentes em ordem FIFO por tenant/device
    @Query("""
            SELECT f FROM FilaSincronizacao f
            WHERE f.tenant.id = :tenantId
            AND (:deviceId IS NULL OR f.deviceId = :deviceId)
            AND f.status = 'PENDENTE'
            ORDER BY f.criadoEm ASC
            """)
    List<FilaSincronizacao> findPendentesByTenant(@Param("tenantId") UUID tenantId,
                                                   @Param("deviceId") String deviceId,
                                                   Pageable pageable);

    @Modifying
    @Query("""
            UPDATE FilaSincronizacao f
            SET f.status = 'PROCESSANDO'
            WHERE f.id IN :ids
            """)
    void marcarComoProcessando(@Param("ids") List<UUID> ids);

    @Modifying
    @Query("""
            UPDATE FilaSincronizacao f
            SET f.status = :status,
                f.erroMensagem = :erro,
                f.tentativas = f.tentativas + 1,
                f.sincronizadoEm = :agora
            WHERE f.id = :id
            """)
    void atualizarStatus(@Param("id") UUID id,
                          @Param("status") StatusSync status,
                          @Param("erro") String erro,
                          @Param("agora") LocalDateTime agora);

    @Query("""
            SELECT COUNT(f) FROM FilaSincronizacao f
            WHERE f.tenant.id = :tenantId
            AND f.status = 'PENDENTE'
            """)
    long countPendentesByTenant(@Param("tenantId") UUID tenantId);

    // Recoloca em PENDENTE itens em PROCESSANDO há mais de 5 min (recuperação de falha)
    @Modifying
    @Query("""
            UPDATE FilaSincronizacao f
            SET f.status = 'PENDENTE'
            WHERE f.status = 'PROCESSANDO'
            AND f.criadoEm < :limite
            AND f.tentativas < :maxTentativas
            """)
    int recolocarProcessandoEmPendente(@Param("limite") LocalDateTime limite,
                                        @Param("maxTentativas") int maxTentativas);

    @Query("""
            SELECT f FROM FilaSincronizacao f
            WHERE f.tenant.id = :tenantId
            AND f.status = 'ERRO'
            AND f.tentativas >= :maxTentativas
            """)
    Page<FilaSincronizacao> findErrosDefinitivos(@Param("tenantId") UUID tenantId,
                                                  @Param("maxTentativas") int maxTentativas,
                                                  Pageable pageable);
}
