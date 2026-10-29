package com.pdv.modules.auth.entity;

import com.pdv.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(nullable = false)
    private boolean revogado;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.revogado == false) {
            this.revogado = false;
        }
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.expiracao);
    }

    public boolean isValido() {
        return !revogado && !isExpirado();
    }
}
