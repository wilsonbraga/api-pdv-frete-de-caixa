package com.pdv.modules.usuario.service;

import com.pdv.modules.auth.repository.RefreshTokenRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.dto.UsuarioDTOs;
import com.pdv.modules.usuario.dto.UsuarioMapper;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioDTOs.Response criar(UUID tenantId, UsuarioDTOs.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (usuarioRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
            throw new BusinessException("Já existe um usuário com o e-mail informado neste tenant");
        }

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setTenant(tenant);
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário criado: {} ({}) no tenant: {}", salvo.getEmail(), salvo.getId(), tenantId);
        return usuarioMapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public UsuarioDTOs.Response buscarPorId(UUID id, UUID tenantId) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public PageResponse<UsuarioDTOs.ResumoResponse> listar(UUID tenantId, String nome,
                                                            String email, Boolean ativo,
                                                            Pageable pageable) {
        Page<Usuario> page = usuarioRepository.findByFiltros(tenantId, nome, email, ativo, pageable);
        return PageResponse.of(page.map(usuarioMapper::toResumoResponse));
    }

    @Transactional
    public UsuarioDTOs.Response atualizar(UUID id, UUID tenantId, UsuarioDTOs.UpdateRequest request) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        // Verifica conflito de e-mail se foi alterado
        if (!usuario.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (usuarioRepository.existsByEmailAndTenantId(request.getEmail(), tenantId)) {
                throw new BusinessException("Já existe um usuário com o e-mail informado");
            }
        }

        usuarioMapper.updateFromRequest(request, usuario);
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário atualizado: {}", salvo.getId());
        return usuarioMapper.toResponse(salvo);
    }

    @Transactional
    public void ativar(UUID id, UUID tenantId) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuário ativado: {}", id);
    }

    @Transactional
    public void desativar(UUID id, UUID tenantId) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        // Revoga todos os tokens ao desativar
        refreshTokenRepository.revogarTodosPorUsuario(id);
        log.info("Usuário desativado: {}", id);
    }

    @Transactional
    public void redefinirSenha(UUID id, UUID tenantId, UsuarioDTOs.RedefinirSenhaRequest request) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);
        // Revoga todos os tokens ao redefinir senha
        refreshTokenRepository.revogarTodosPorUsuario(id);
        log.info("Senha redefinida para usuário: {}", id);
    }
}
