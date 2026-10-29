package com.pdv.modules.sincronizacao.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.tenant.entity.Tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fila_sincronizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaSincronizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdv_id")
    private PDV pdv;

    @Column(nullable = false, length = 100)
    private String entidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperacaoSync operacao;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSync status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "sincronizado_em")
    private LocalDateTime sincronizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) this.status = StatusSync.PENDENTE;
        if (this.tentativas == 0) this.tentativas = 0;
    }
}
