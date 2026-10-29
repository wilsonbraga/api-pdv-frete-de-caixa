package com.pdv.modules.auth.repository;

import com.pdv.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revogado = true WHERE rt.usuario.id = :usuarioId")
    void revogarTodosPorUsuario(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revogado = true WHERE rt.token = :token")
    void revogarPorToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiracao < :agora OR rt.revogado = true")
    int limparExpiradosERevogados(@Param("agora") LocalDateTime agora);

    @Query("""
            SELECT COUNT(rt) > 0 FROM RefreshToken rt
            WHERE rt.usuario.id = :usuarioId
            AND rt.revogado = false
            AND rt.expiracao > :agora
            """)
    boolean existeTokenValidoPorUsuario(@Param("usuarioId") UUID usuarioId,
                                        @Param("agora") LocalDateTime agora);
}
