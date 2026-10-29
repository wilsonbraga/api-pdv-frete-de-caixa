package com.pdv.modules.auth.security;

import com.pdv.modules.usuario.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final String nome;
    private final String email;
    private final String password;
    private final boolean ativo;
    private final String role;
    private final Double descontoMax;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(UUID id, UUID tenantId, String nome, String email,
                          String password, boolean ativo, String role, Double descontoMax) {
        this.id = id;
        this.tenantId = tenantId;
        this.nome = nome;
        this.email = email;
        this.password = password;
        this.ativo = ativo;
        this.role = role;
        this.descontoMax = descontoMax;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public static UserPrincipal of(Usuario usuario) {
        return new UserPrincipal(
                usuario.getId(),
                usuario.getTenant().getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSenhaHash(),
                usuario.isAtivo(),
                usuario.getRole().name(),
                usuario.getDescontoMax()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    public boolean hasRole(String roleName) {
        return this.role.equals(roleName);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    public boolean isGerente() {
        return "GERENTE".equals(this.role) || isAdmin();
    }
}
