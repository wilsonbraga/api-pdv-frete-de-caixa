package com.pdv.modules.pdv.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pdv.modules.pdv.entity.PDV;

@Repository
public interface PDVRepository extends JpaRepository<PDV, UUID> {

    Optional<PDV> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PDV> findByDeviceIdAndTenantId(String deviceId, UUID tenantId);

    boolean existsByDeviceId(String deviceId);
    
    List<PDV> findByTenantIdAndAtivoTrue(UUID tenantId);
}
