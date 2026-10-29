package com.pdv.modules.fornecedor.entity;

import com.pdv.modules.produto.entity.Produto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "preco_custo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "total_item", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalItem;

    public void recalcularTotal() {
        this.totalItem = this.precoCusto.multiply(this.quantidade);
    }
}
