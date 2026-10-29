package com.pdv.modules.venda.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.pdv.modules.caixa.entity.Caixa;
import com.pdv.modules.cliente.entity.Cliente;
import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.shared.entity.BaseEntity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venda extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caixa_id", nullable = false)
    private Caixa caixa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pdv_id", nullable = false)
    private PDV pdv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operador_id", nullable = false)
    private Usuario operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "numero_venda", nullable = false, length = 30)
    private String numeroVenda;

    @Column(name = "dt_venda", nullable = false)
    private LocalDateTime dtVenda;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVenda status;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private Usuario canceladoPor;

    @Column(name = "dt_cancelamento")
    private LocalDateTime dtCancelamento;

    @Column(nullable = false)
    private boolean sincronizado;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Version
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC")
    private Set<ItemVenda> itens = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("criadoEm ASC")
    private Set<PagamentoVenda> pagamentos = new LinkedHashSet<>();

    public BigDecimal calcularSubtotal() {
        return itens.stream()
                .filter(i -> !i.isCancelado())
                .map(ItemVenda::getTotalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularTotal() {
        return calcularSubtotal().subtract(desconto);
    }

    public BigDecimal getTotalPago() {
        return pagamentos.stream()
                .map(PagamentoVenda::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTroco() {
        BigDecimal totalPago = getTotalPago();
        BigDecimal diferenca = totalPago.subtract(total);
        return diferenca.compareTo(BigDecimal.ZERO) > 0 ? diferenca : BigDecimal.ZERO;
    }
}
