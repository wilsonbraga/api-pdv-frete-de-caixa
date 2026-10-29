package com.pdv.modules.pix.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.venda.entity.Venda;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cobranca_pix")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CobrancaPix {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id")
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "configuracao_pix_id", nullable = false)
    private ConfiguracaoPix configuracaoPix;

    // Identificadores Mercado Pago
    @Column(name = "mp_payment_id", unique = true, length = 100)
    private String mpPaymentId;

    @Column(name = "mp_preference_id", length = 100)
    private String mpPreferenceId;

    // Dados da cobrança
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(length = 255)
    private String descricao;

    // QR Code
    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "ticket_url", length = 500)
    private String ticketUrl;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCobrancaPix status;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webhook_payload", columnDefinition = "jsonb")
    private String webhookPayload;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public boolean isPendente() {
        return StatusCobrancaPix.PENDENTE.equals(this.status);
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.expiracao);
    }
}
