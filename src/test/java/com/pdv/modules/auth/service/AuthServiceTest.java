package com.pdv.modules.auth.service;

import com.pdv.modules.auth.dto.AuthDTOs;
import com.pdv.modules.auth.entity.RefreshToken;
import com.pdv.modules.auth.repository.RefreshTokenRepository;
import com.pdv.modules.auth.security.JwtService;
import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.RoleUsuario;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UUID tenantId;
    private UUID usuarioId;
    private Tenant tenant;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        usuarioId = UUID.randomUUID();

        tenant = Tenant.builder()
                .id(tenantId)
                .nome("Loja Teste")
                .ativo(true)
                .plano("BASICO")
                .build();

        usuario = Usuario.builder()
                .id(usuarioId)
                .tenant(tenant)
                .nome("Admin Teste")
                .email("admin@teste.com")
                .senhaHash("$2a$12$hashed")
                .role(RoleUsuario.ADMIN)
                .ativo(true)
                .descontoMax(10.0)
                .version(0L)
                .build();
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("deve retornar tokens ao autenticar com credenciais válidas")
        void deveRetornarTokens_quandoCredenciaisValidas() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setEmail("admin@teste.com");
            request.setSenha("senha123");
            request.setTenantId(tenantId.toString());
            request.setDeviceId("DEVICE-001");

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByEmailAndTenantId("admin@teste.com", tenantId))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("senha123", "$2a$12$hashed")).thenReturn(true);
            when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("refresh-token");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(i -> i.getArgument(0));

            AuthDTOs.TokenResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUsuario().getEmail()).isEqualTo("admin@teste.com");
            assertThat(response.getUsuario().getRole()).isEqualTo("ADMIN");

            verify(usuarioRepository).atualizarUltimoLogin(
                    eq(usuarioId), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("deve lançar BadCredentialsException quando senha incorreta")
        void deveLancarBadCredentials_quandoSenhaIncorreta() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setEmail("admin@teste.com");
            request.setSenha("senhaErrada");
            request.setTenantId(tenantId.toString());

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByEmailAndTenantId("admin@teste.com", tenantId))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("senhaErrada", "$2a$12$hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("deve lançar DisabledException quando usuário inativo")
        void deveLancarDisabled_quandoUsuarioInativo() {
            usuario.setAtivo(false);

            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setEmail("admin@teste.com");
            request.setSenha("senha123");
            request.setTenantId(tenantId.toString());

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(usuarioRepository.findByEmailAndTenantId("admin@teste.com", tenantId))
                    .thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(DisabledException.class);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando tenant não existe")
        void deveLancarNotFound_quandoTenantInexistente() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setEmail("admin@teste.com");
            request.setSenha("senha123");
            request.setTenantId(tenantId.toString());

            when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando tenantId tem formato inválido")
        void deveLancarBusiness_quandoTenantIdInvalido() {
            AuthDTOs.LoginRequest request = new AuthDTOs.LoginRequest();
            request.setEmail("admin@teste.com");
            request.setSenha("senha123");
            request.setTenantId("id-invalido");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tenant ID inválido");
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTest {

        @Test
        @DisplayName("deve renovar tokens quando refresh token válido")
        void deveRenovarTokens_quandoRefreshValido() {
            // Cria RefreshToken usando a entidade real
            RefreshToken rt = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .usuario(usuario)
                    .token("valid-refresh-token")
                    .expiracao(LocalDateTime.now().plusDays(7))
                    .revogado(false)
                    .build();

            AuthDTOs.RefreshTokenRequest request = new AuthDTOs.RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");

            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(rt));
            when(jwtService.generateAccessToken(any())).thenReturn("new-access");
            when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
            when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(i -> i.getArgument(0));

            AuthDTOs.TokenResponse response = authService.refreshToken(request);

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            verify(refreshTokenRepository).revogarPorToken("valid-refresh-token");
        }

        @Test
        @DisplayName("deve lançar BusinessException quando refresh token expirado")
        void deveLancarBusiness_quandoRefreshExpirado() {
            RefreshToken rt = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .usuario(usuario)
                    .token("expired-token")
                    .expiracao(LocalDateTime.now().minusDays(1))
                    .revogado(false)
                    .build();

            AuthDTOs.RefreshTokenRequest request = new AuthDTOs.RefreshTokenRequest();
            request.setRefreshToken("expired-token");

            when(refreshTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(rt));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expirado");
        }
    }

    @Nested
    @DisplayName("alterarSenha()")
    class AlterarSenha {

        @Test
        @DisplayName("deve alterar senha e revogar todos os tokens")
        void deveAlterarSenha_eRevogarTokens() {
            AuthDTOs.AlterarSenhaRequest request = new AuthDTOs.AlterarSenhaRequest();
            request.setSenhaAtual("senhaAtual");
            request.setNovaSenha("novaSenha123");

            when(usuarioRepository.findByIdAndTenantId(usuarioId, tenantId))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("senhaAtual", "$2a$12$hashed")).thenReturn(true);
            when(passwordEncoder.encode("novaSenha123")).thenReturn("$2a$12$novoHash");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

            authService.alterarSenha(usuarioId, tenantId, request);

            assertThat(usuario.getSenhaHash()).isEqualTo("$2a$12$novoHash");
            verify(refreshTokenRepository).revogarTodosPorUsuario(usuarioId);
        }

        @Test
        @DisplayName("deve lançar BusinessException quando senha atual incorreta")
        void deveLancarBusiness_quandoSenhaAtualIncorreta() {
            AuthDTOs.AlterarSenhaRequest request = new AuthDTOs.AlterarSenhaRequest();
            request.setSenhaAtual("senhaErrada");
            request.setNovaSenha("nova123");

            when(usuarioRepository.findByIdAndTenantId(usuarioId, tenantId))
                    .thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("senhaErrada", "$2a$12$hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.alterarSenha(usuarioId, tenantId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Senha atual incorreta");
        }
    }
}
