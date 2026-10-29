package com.pdv.modules.usuario.entity;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleUsuario role;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "desconto_max", nullable = false)
    private Double descontoMax;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Version
    private Long version;
}
