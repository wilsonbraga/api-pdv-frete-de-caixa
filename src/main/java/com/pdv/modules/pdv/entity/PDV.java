package com.pdv.modules.pdv.entity;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "pdv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PDV extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(name = "device_id", unique = true, length = 100)
    private String deviceId;

    @Column(nullable = false)
    private boolean ativo;
}
