package com.pdv.modules.cliente.entity;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf_cnpj", length = 18)
    private String cpfCnpj;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 255)
    private String endereco;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column(length = 10)
    private String cep;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "limite_credito", nullable = false, precision = 12, scale = 2)
    private BigDecimal limiteCredito;

    @Column(name = "saldo_devedor", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDevedor;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(nullable = false)
    private boolean ativo;

    @Version
    private Long version;

    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(saldoDevedor);
    }

    public boolean possuiCreditoSuficiente(BigDecimal valor) {
        return getCreditoDisponivel().compareTo(valor) >= 0;
    }
}
