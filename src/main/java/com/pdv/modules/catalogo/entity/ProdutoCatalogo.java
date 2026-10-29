package com.pdv.modules.catalogo.entity;

import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "produto_catalogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoCatalogo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CatalogoCategoria categoria;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(name = "codigo_barras", length = 50)
    private String codigoBarras;

    @Column(length = 10)
    private String ncm;

    @Column(nullable = false, length = 10)
    private String unidade;

    @Column(name = "preco_sugerido", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoSugerido;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private boolean ativo;
}
