package com.pdv.modules.produto.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ProdutoDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 200)
        private String nome;

        private String descricao;

        @Size(max = 50)
        private String codigoBarras;

        @Size(max = 50)
        private String sku;

        @Size(max = 10)
        private String ncm;

        @NotBlank(message = "Unidade é obrigatória")
        @Size(max = 10)
        private String unidade;

        @NotNull(message = "Preço de custo é obrigatório")
        @DecimalMin(value = "0.0", message = "Preço de custo não pode ser negativo")
        private BigDecimal precoCusto;

        @NotNull(message = "Preço de venda é obrigatório")
        @DecimalMin(value = "0.0", message = "Preço de venda não pode ser negativo")
        private BigDecimal precoVenda;

        @DecimalMin(value = "0.0")
        private BigDecimal estoqueAtual = BigDecimal.ZERO;

        @DecimalMin(value = "0.0")
        private BigDecimal estoqueMinimo = BigDecimal.ZERO;

        private UUID categoriaId;

        /** Fornecedor principal — opcional */
        private UUID fornecedorId;

        private boolean composto = false;

        private List<ComponenteRequest> componentes;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 200)
        private String nome;

        private String descricao;

        @Size(max = 50)
        private String codigoBarras;

        @Size(max = 50)
        private String sku;

        @Size(max = 10)
        private String ncm;

        @NotBlank(message = "Unidade é obrigatória")
        @Size(max = 10)
        private String unidade;

        @NotNull(message = "Preço de custo é obrigatório")
        @DecimalMin(value = "0.0")
        private BigDecimal precoCusto;

        @NotNull(message = "Preço de venda é obrigatório")
        @DecimalMin(value = "0.0")
        private BigDecimal precoVenda;

        @DecimalMin(value = "0.0")
        private BigDecimal estoqueMinimo;

        private UUID categoriaId;

        /** Fornecedor principal — opcional */
        private UUID fornecedorId;

        private List<ComponenteRequest> componentes;
    }

    @Data
    public static class ComponenteRequest {
        @NotNull(message = "Produto filho é obrigatório")
        private UUID produtoFilhoId;

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        private BigDecimal quantidade;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID tenantId;
        private String nome;
        private String descricao;
        private String codigoBarras;
        private String sku;
        private String ncm;
        private String unidade;
        private BigDecimal precoCusto;
        private BigDecimal precoVenda;
        private BigDecimal margemLucro;
        private BigDecimal estoqueAtual;
        private BigDecimal estoqueMinimo;
        private boolean estoqueAbaixoMinimo;
        private String fotoUrl;
        private boolean ativo;
        private boolean composto;
        private CategoriaResumo categoria;
        private FornecedorResumo fornecedor;
        private List<ComponenteResponse> componentes;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private String nome;
        private String codigoBarras;
        private String sku;
        private String unidade;
        private BigDecimal precoVenda;
        private BigDecimal estoqueAtual;
        private BigDecimal estoqueMinimo;
        private boolean estoqueAbaixoMinimo;
        private boolean ativo;
        private CategoriaResumo categoria;
        private FornecedorResumo fornecedor;
    }

    @Data
    public static class ComponenteResponse {
        private UUID id;
        private UUID produtoFilhoId;
        private String nomeProdutoFilho;
        private BigDecimal quantidade;
    }

    @Data
    public static class CategoriaResumo {
        private UUID id;
        private String nome;
    }

    /** Resumo do fornecedor exibido junto ao produto */
    @Data
    public static class FornecedorResumo {
        private UUID id;
        private String razaoSocial;
        private String nomeFantasia;
        private String telefone;
        private String whatsappUrl;   // montado no service: https://wa.me/55XXXXXXXXXX
    }

    @Data
    public static class HistoricoPrecoResponse {
        private UUID id;
        private BigDecimal precoCusto;
        private BigDecimal precoVenda;
        private String nomeUsuario;
        private LocalDateTime criadoEm;
    }
}
