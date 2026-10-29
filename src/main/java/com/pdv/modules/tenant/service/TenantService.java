package com.pdv.modules.tenant.service;

import com.pdv.modules.tenant.dto.TenantDTOs;
import com.pdv.modules.tenant.dto.TenantMapper;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Transactional
    public TenantDTOs.Response criar(TenantDTOs.CreateRequest request) {
        if (request.getCnpj() != null && tenantRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("Já existe um tenant cadastrado com o CNPJ informado");
        }
        Tenant tenant = tenantMapper.toEntity(request);
        Tenant salvo = tenantRepository.save(tenant);
        log.info("Tenant criado: {} ({})", salvo.getNome(), salvo.getId());
        return tenantMapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public TenantDTOs.Response buscarPorId(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        return tenantMapper.toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantDTOs.ResumoResponse> listar(String nome, Boolean ativo, Pageable pageable) {
        Page<Tenant> page = tenantRepository.findByFiltros(nome, ativo, pageable);
        return PageResponse.of(page.map(tenantMapper::toResumoResponse));
    }

    @Transactional
    public TenantDTOs.Response atualizar(UUID id, TenantDTOs.UpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        tenantMapper.updateFromRequest(request, tenant);
        Tenant salvo = tenantRepository.save(tenant);
        log.info("Tenant atualizado: {}", salvo.getId());
        return tenantMapper.toResponse(salvo);
    }

    @Transactional
    public void ativar(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        tenant.setAtivo(true);
        tenantRepository.save(tenant);
        log.info("Tenant ativado: {}", id);
    }

    @Transactional
    public void desativar(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        tenant.setAtivo(false);
        tenantRepository.save(tenant);
        log.info("Tenant desativado: {}", id);
    }

    @Transactional
    public void atualizarLogo(UUID id, String logoUrl) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        tenant.setLogoUrl(logoUrl);
        tenantRepository.save(tenant);
    }
}
