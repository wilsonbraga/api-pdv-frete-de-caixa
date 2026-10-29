package com.pdv.modules.usuario.repository;

import com.pdv.modules.usuario.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Usuario> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findById(UUID id);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    Page<Usuario> findByTenantIdAndAtivoTrue(UUID tenantId, Pageable pageable);

    Page<Usuario> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.tenant.id = :tenantId
            AND (CAST(:nome AS string) IS NULL
                 OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
            AND (CAST(:email AS string) IS NULL
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS string), '%')))
            AND (:ativo IS NULL OR u.ativo = :ativo)
            """)
    Page<Usuario> findByFiltros(@Param("tenantId") UUID tenantId,
                                @Param("nome") String nome,
                                @Param("email") String email,
                                @Param("ativo") Boolean ativo,
                                Pageable pageable);

    @Modifying
    @Query("UPDATE Usuario u SET u.ultimoLogin = :dataLogin WHERE u.id = :id")
    void atualizarUltimoLogin(@Param("id") UUID id,
                               @Param("dataLogin") LocalDateTime dataLogin);
}