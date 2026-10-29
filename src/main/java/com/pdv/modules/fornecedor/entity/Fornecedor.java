package com.pdv.modules.fornecedor.entity;

import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "fornecedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fornecedor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Column(length = 18)
    private String cnpj;

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

    @Column(length = 150)
    private String contato;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(nullable = false)
    private boolean ativo;
}
