package com.pdv.modules.auth.service;

import com.pdv.modules.auth.dto.AuthDTOs;
import com.pdv.modules.auth.entity.RefreshToken;
import com.pdv.modules.auth.repository.RefreshTokenRepository;
import com.pdv.modules.auth.security.JwtService;
import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthDTOs.TokenResponse login(AuthDTOs.LoginRequest request) {
    	// Buscar usuário pelo email primeiro (sem tenantId)
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));

        if (!usuario.isAtivo()) {
            throw new DisabledException("Conta da empresa está inativa");
        }
        
     // Verificar se o tenant está ativo
        Tenant tenant = usuario.getTenant();
        if (!tenant.isAtivo()) {
            throw new DisabledException("Conta da empresa está inativa");
        }
        
        // Se o tenantId foi enviado, validar se corresponde ao usuário
        if (request.getTenantId() != null && !request.getTenantId().isEmpty()) {
            UUID requestTenantId = UUID.fromString(request.getTenantId());
            if (!tenant.getId().equals(requestTenantId)) {
                throw new BadCredentialsException("Credenciais inválidas");
            }
        }
        
        // Validar senha
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos");
        }
        
        UUID tenantId = tenant.getId();
        UserPrincipal principal = UserPrincipal.of(usuario);
        
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshTokenValue = jwtService.generateRefreshToken(principal);
        
        // Persiste refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(refreshTokenValue)
                .expiracao(LocalDateTime.now().plusSeconds(
                        jwtService.getAccessTokenExpirationMs() / 1000 * 560))
                .revogado(false)
                .deviceId(request.getDeviceId())
                .build();
        refreshTokenRepository.save(refreshToken);
        
        // Atualiza último login
        usuarioRepository.atualizarUltimoLogin(usuario.getId(), LocalDateTime.now());
        
        log.info("Login realizado — usuário: {} | tenant: {}", usuario.getEmail(), tenantId);
        
        return buildTokenResponse(accessToken, refreshTokenValue, principal);
    }

    @Transactional
    public AuthDTOs.TokenResponse refreshToken(AuthDTOs.RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Refresh token inválido ou não encontrado"));

        if (!stored.isValido()) {
            throw new BusinessException("Refresh token expirado ou revogado. Faça login novamente");
        }

        Usuario usuario = stored.getUsuario();

        if (!usuario.isAtivo()) {
            refreshTokenRepository.revogarTodosPorUsuario(usuario.getId());
            throw new DisabledException("Usuário inativo. Entre em contato com o administrador");
        }

        // Revoga o token atual (rotação de tokens)
        refreshTokenRepository.revogarPorToken(request.getRefreshToken());

        UserPrincipal principal = UserPrincipal.of(usuario);
        String newAccessToken = jwtService.generateAccessToken(principal);
        String newRefreshTokenValue = jwtService.generateRefreshToken(principal);

        // Persiste novo refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(newRefreshTokenValue)
                .expiracao(stored.getExpiracao()) // mantém mesma expiração original
                .revogado(false)
                .deviceId(stored.getDeviceId())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("Refresh token renovado — usuário: {}", usuario.getEmail());

        return buildTokenResponse(newAccessToken, newRefreshTokenValue, principal);
    }

    @Transactional
    public void logout(AuthDTOs.LogoutRequest request) {
        refreshTokenRepository.revogarPorToken(request.getRefreshToken());
        log.info("Logout realizado — token revogado");
    }

    @Transactional
    public void logoutTodosDispositivos(UUID usuarioId) {
        refreshTokenRepository.revogarTodosPorUsuario(usuarioId);
        log.info("Logout em todos os dispositivos — usuário: {}", usuarioId);
    }

    @Transactional
    public void alterarSenha(UUID usuarioId, UUID tenantId, AuthDTOs.AlterarSenhaRequest request) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenhaHash())) {
            throw new BusinessException("Senha atual incorreta");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);

        // Revoga todos os tokens ao trocar a senha (segurança)
        refreshTokenRepository.revogarTodosPorUsuario(usuarioId);

        log.info("Senha alterada — usuário: {}", usuarioId);
    }

    @Transactional(readOnly = true)
    public AuthDTOs.ValidarPinResponse validarPin(UUID usuarioId, UUID tenantId,
                                                  AuthDTOs.ValidarPinRequest request) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        if (usuario.getPinHash() == null) {
            AuthDTOs.ValidarPinResponse response = new AuthDTOs.ValidarPinResponse();
            response.setValido(false);
            response.setMensagem("PIN não cadastrado para este usuário");
            return response;
        }

        boolean pinValido = passwordEncoder.matches(request.getPin(), usuario.getPinHash());

        AuthDTOs.ValidarPinResponse response = new AuthDTOs.ValidarPinResponse();
        response.setValido(pinValido);
        response.setMensagem(pinValido ? "PIN válido" : "PIN inválido");

        log.info("Validação de PIN para ação '{}' — usuário: {} — resultado: {}",
                request.getAcao(), usuarioId, pinValido ? "OK" : "INVÁLIDO");

        return response;
    }

    @Transactional
    public void cadastrarPin(UUID usuarioId, UUID tenantId, String pin) {
        Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        usuario.setPinHash(passwordEncoder.encode(pin));
        usuarioRepository.save(usuario);

        log.info("PIN cadastrado/atualizado — usuário: {}", usuarioId);
    }

    // Limpeza de tokens expirados — executa diariamente às 03:00
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void limparRefreshTokensExpirados() {
        int removidos = refreshTokenRepository.limparExpiradosERevogados(LocalDateTime.now());
        log.info("Limpeza de refresh tokens: {} registros removidos", removidos);
    }

    private AuthDTOs.TokenResponse buildTokenResponse(String accessToken, String refreshToken,
                                                       UserPrincipal principal) {
        AuthDTOs.TokenResponse.UsuarioInfo usuarioInfo = new AuthDTOs.TokenResponse.UsuarioInfo();
        usuarioInfo.setId(principal.getId().toString());
        usuarioInfo.setNome(principal.getNome());
        usuarioInfo.setEmail(principal.getEmail());
        usuarioInfo.setRole(principal.getRole());
        usuarioInfo.setTenantId(principal.getTenantId().toString());
        usuarioInfo.setDescontoMax(principal.getDescontoMax());

        AuthDTOs.TokenResponse response = new AuthDTOs.TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtService.getAccessTokenExpirationMs() / 1000);
        response.setUsuario(usuarioInfo);

        return response;
    }
}
