package com.pdv.modules.pix.entity;

import java.util.UUID;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.shared.entity.BaseEntity;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracao_pix")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoPix extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProvedorPix provedor;

    @Column(nullable = false)
    private boolean ativo;

    // Mercado Pago
    @Column(name = "mp_access_token", length = 500)
    private String mpAccessToken;

    @Column(name = "mp_public_key", length = 255)
    private String mpPublicKey;

    @Column(name = "mp_user_id", length = 50)
    private String mpUserId;

    // Chave PIX
    @Column(name = "chave_pix", length = 150)
    private String chavePix;

    @Column(name = "tipo_chave", length = 20)
    private String tipoChave;

    @Column(name = "nome_beneficiario", length = 150)
    private String nomeBeneficiario;

    @Column(name = "descricao_padrao", length = 255)
    private String descricaoPadrao;

    @Column(nullable = false)
    private boolean sandbox;
}
