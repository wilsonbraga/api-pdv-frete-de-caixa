package com.pdv.modules.cliente.repository;

import com.pdv.modules.cliente.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Cliente> findByCpfCnpjAndTenantId(String cpfCnpj, UUID tenantId);

    boolean existsByCpfCnpjAndTenantId(String cpfCnpj, UUID tenantId);

    @Query("""
            SELECT c FROM Cliente c
            WHERE c.tenant.id = :tenantId
            AND (CAST(:nome AS string) IS NULL
                 OR LOWER(c.nome) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))
            AND (CAST(:cpfCnpj AS string) IS NULL OR c.cpfCnpj = CAST(:cpfCnpj AS string))
            AND (:ativo IS NULL OR c.ativo = :ativo)
            AND (:comSaldoDevedor IS NULL OR
                 (:comSaldoDevedor = true AND c.saldoDevedor > 0))
            """)
    Page<Cliente> findByFiltros(@Param("tenantId") UUID tenantId,
                                @Param("nome") String nome,
                                @Param("cpfCnpj") String cpfCnpj,
                                @Param("ativo") Boolean ativo,
                                @Param("comSaldoDevedor") Boolean comSaldoDevedor,
                                Pageable pageable);
}