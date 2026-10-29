package com.pdv.modules.pdv.service;

import com.pdv.modules.pdv.dto.PDVDTOs;
import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.pdv.repository.PDVRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDVService {

    private final PDVRepository  pdvRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<PDVDTOs.Response> listarAtivos(UUID tenantId) {
        return pdvRepository.findByTenantIdAndAtivoTrue(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PDVDTOs.Response buscarPorId(UUID id, UUID tenantId) {
        PDV pdv = pdvRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PDV", id));
        return toResponse(pdv);
    }

    @Transactional
    public PDVDTOs.Response criar(UUID tenantId, PDVDTOs.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (request.getDeviceId() != null
                && !request.getDeviceId().isBlank()
                && pdvRepository.existsByDeviceId(request.getDeviceId())) {
            throw new BusinessException(
                    "Já existe um PDV cadastrado com o deviceId: " + request.getDeviceId());
        }

        PDV pdv = PDV.builder()
                .tenant(tenant)
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .deviceId(request.getDeviceId())
                .ativo(true)
                .build();

        PDV salvo = pdvRepository.save(pdv);
        log.info("PDV criado: {} ({}) tenant: {}", salvo.getNome(), salvo.getId(), tenantId);
        return toResponse(salvo);
    }

    @Transactional
    public PDVDTOs.Response atualizar(UUID id, UUID tenantId, PDVDTOs.UpdateRequest request) {
        PDV pdv = pdvRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PDV", id));
        pdv.setNome(request.getNome());
        pdv.setDescricao(request.getDescricao());
        return toResponse(pdvRepository.save(pdv));
    }

    @Transactional
    public void desativar(UUID id, UUID tenantId) {
        PDV pdv = pdvRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PDV", id));
        pdv.setAtivo(false);
        pdvRepository.save(pdv);
        log.info("PDV desativado: {}", id);
    }

    private PDVDTOs.Response toResponse(PDV pdv) {
        PDVDTOs.Response r = new PDVDTOs.Response();
        r.setId(pdv.getId());
        r.setNome(pdv.getNome());
        r.setDescricao(pdv.getDescricao());
        r.setDeviceId(pdv.getDeviceId());
        r.setAtivo(pdv.isAtivo());
        return r;
    }
}