package com.pdv.modules.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 100)
    private String acao;

    @Column(nullable = false, length = 100)
    private String entidade;

    @Column(name = "entidade_id", length = 100)
    private String entidadeId;

    @Column(name = "payload_antes", columnDefinition = "jsonb")
    private String payloadAntes;

    @Column(name = "payload_depois", columnDefinition = "jsonb")
    private String payloadDepois;

    @Column(length = 50)
    private String ip;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }
}
