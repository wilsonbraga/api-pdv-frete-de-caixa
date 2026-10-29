package com.pdv.modules.venda.entity;

import com.pdv.modules.produto.entity.Produto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "item_venda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    // Snapshotted para preservar histórico mesmo que o produto mude
    @Column(name = "nome_produto", nullable = false, length = 200)
    private String nomeProduto;

    @Column(name = "codigo_barras", length = 50)
    private String codigoBarras;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "desconto_item", nullable = false, precision = 12, scale = 2)
    private BigDecimal descontoItem;

    @Column(name = "total_item", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalItem;

    @Column(nullable = false)
    private boolean cancelado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    public void recalcularTotal() {
        BigDecimal bruto = precoUnitario.multiply(quantidade);
        this.totalItem = bruto.subtract(descontoItem);
    }
}
