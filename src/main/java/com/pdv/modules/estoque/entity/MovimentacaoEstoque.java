package com.pdv.modules.estoque.entity;

import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "movimentacao_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "quantidade_anterior", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeAnterior;

    @Column(name = "quantidade_posterior", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadePosterior;

    @Column(length = 255)
    private String motivo;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
