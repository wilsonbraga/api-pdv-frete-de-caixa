package com.pdv.modules.pix.repository;

import com.pdv.modules.pix.entity.ConfiguracaoPix;
import com.pdv.modules.pix.entity.ProvedorPix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfiguracaoPixRepository extends JpaRepository<ConfiguracaoPix, UUID> {

    List<ConfiguracaoPix> findByTenantIdAndAtivoTrue(UUID tenantId);

    Optional<ConfiguracaoPix> findByTenantIdAndProvedor(UUID tenantId, ProvedorPix provedor);

    Optional<ConfiguracaoPix> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndProvedor(UUID tenantId, ProvedorPix provedor);
}
