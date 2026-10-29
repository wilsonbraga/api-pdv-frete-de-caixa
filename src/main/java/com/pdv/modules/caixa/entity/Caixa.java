package com.pdv.modules.caixa.entity;

import com.pdv.modules.pdv.entity.PDV;
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
@Table(name = "caixa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caixa extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pdv_id", nullable = false)
    private PDV pdv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id", nullable = false)
    private Usuario operador;

    @Column(name = "dt_abertura", nullable = false)
    private LocalDateTime dtAbertura;

    @Column(name = "dt_fechamento")
    private LocalDateTime dtFechamento;

    @Column(name = "valor_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorInicial;

    @Column(name = "total_dinheiro", precision = 12, scale = 2)
    private BigDecimal totalDinheiro;

    @Column(name = "total_debito", precision = 12, scale = 2)
    private BigDecimal totalDebito;

    @Column(name = "total_credito", precision = 12, scale = 2)
    private BigDecimal totalCredito;

    @Column(name = "total_pix", precision = 12, scale = 2)
    private BigDecimal totalPix;

    @Column(name = "total_fiado", precision = 12, scale = 2)
    private BigDecimal totalFiado;

    @Column(name = "total_sangria", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSangria;

    @Column(name = "total_suprimento", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSuprimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCaixa status;

    @Column(name = "observacao_fechamento", columnDefinition = "TEXT")
    private String observacaoFechamento;

    @Builder.Default
    @OneToMany(mappedBy = "caixa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovimentacaoCaixa> movimentacoes = new ArrayList<>();

    public boolean isAberto() {
        return StatusCaixa.ABERTO.equals(this.status);
    }

    public BigDecimal getTotalVendas() {
        BigDecimal total = BigDecimal.ZERO;
        if (totalDinheiro != null) total = total.add(totalDinheiro);
        if (totalDebito != null) total = total.add(totalDebito);
        if (totalCredito != null) total = total.add(totalCredito);
        if (totalPix != null) total = total.add(totalPix);
        if (totalFiado != null) total = total.add(totalFiado);
        return total;
    }

    public BigDecimal getSaldoDinheiro() {
        BigDecimal saldo = valorInicial;
        if (totalDinheiro != null) saldo = saldo.add(totalDinheiro);
        if (totalSangria != null) saldo = saldo.subtract(totalSangria);
        if (totalSuprimento != null) saldo = saldo.add(totalSuprimento);
        return saldo;
    }
}
