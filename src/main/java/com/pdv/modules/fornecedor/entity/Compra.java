package com.pdv.modules.fornecedor.entity;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(name = "numero_nota", length = 50)
    private String numeroNota;

    @Column(name = "dt_compra", nullable = false)
    private LocalDateTime dtCompra;

    @Column(name = "dt_entrega")
    private LocalDateTime dtEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCompra status;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Builder.Default
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCompra> itens = new ArrayList<>();

    public BigDecimal calcularTotal() {
        return itens.stream()
                .map(ItemCompra::getTotalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
