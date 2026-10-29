package com.pdv.modules.produto.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.pdv.modules.fornecedor.entity.Fornecedor;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.shared.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaProduto categoria;
    
    /** Fornecedor principal — opcional. Usado nos alertas de estoque mínimo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "codigo_barras", length = 50)
    private String codigoBarras;

    @Column(length = 50)
    private String sku;

    @Column(length = 10)
    private String ncm;

    @Column(nullable = false, length = 10)
    private String unidade;

    @Column(name = "preco_custo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoCusto;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    // margem_lucro é coluna GENERATED no banco — leitura apenas
    @Column(name = "margem_lucro", insertable = false, updatable = false)
    private BigDecimal margemLucro;

    @Column(name = "estoque_atual", nullable = false, precision = 12, scale = 3)
    private BigDecimal estoqueAtual;

    @Column(name = "estoque_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal estoqueMinimo;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private boolean composto;

    @Version
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "produtoPai", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProdutoComponente> componentes = new ArrayList<>();

    public boolean estoqueInsuficiente(BigDecimal quantidade) {
        return this.estoqueAtual.compareTo(quantidade) < 0;
    }

    public boolean estoqueAbaixoMinimo() {
        return this.estoqueAtual.compareTo(this.estoqueMinimo) <= 0;
    }
}
