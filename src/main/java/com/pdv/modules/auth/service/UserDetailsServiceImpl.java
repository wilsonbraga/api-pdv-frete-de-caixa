package com.pdv.modules.auth.service;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Chamado pelo JwtAuthenticationFilter — recebe o ID do usuário (subject do JWT).
     * Chamado pelo DaoAuthenticationProvider — recebe o email durante o login.
     * Suporta os dois formatos.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String idOrEmail) throws UsernameNotFoundException {
        Usuario usuario;

        // Tenta carregar por UUID primeiro (fluxo JWT)
        try {
            UUID id = UUID.fromString(idOrEmail);
            usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuário não encontrado com id: " + id));
        } catch (IllegalArgumentException e) {
            // Não é UUID — tenta por e-mail (fluxo de login inicial é tratado no AuthService)
            throw new UsernameNotFoundException("Formato de identificador inválido: " + idOrEmail);
        }

        return UserPrincipal.of(usuario);
    }

    /**
     * Carrega por e-mail + tenantId — usado pelo AuthService no login.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmailAndTenant(String email, UUID tenantId)
            throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado com e-mail: " + email));
        return UserPrincipal.of(usuario);
    }
}
