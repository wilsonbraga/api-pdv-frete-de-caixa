package com.pdv.modules.produto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "produto_componente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoComponente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_pai_id", nullable = false)
    private Produto produtoPai;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_filho_id", nullable = false)
    private Produto produtoFilho;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;
}
