package com.pdv.modules.fornecedor.dto;

import com.pdv.modules.fornecedor.entity.StatusCompra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FornecedorDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200)
        private String razaoSocial;

        @Size(max = 200)
        private String nomeFantasia;

        @Size(max = 18)
        private String cnpj;

        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 20)
        private String telefone;

        @Size(max = 255)
        private String endereco;

        @Size(max = 100)
        private String cidade;

        @Size(max = 2)
        private String estado;

        @Size(max = 10)
        private String cep;

        @Size(max = 150)
        private String contato;

        private String observacao;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Razão social é obrigatória")
        @Size(max = 200)
        private String razaoSocial;

        @Size(max = 200)
        private String nomeFantasia;

        @Email(message = "E-mail inválido")
        @Size(max = 150)
        private String email;

        @Size(max = 20)
        private String telefone;

        @Size(max = 255)
        private String endereco;

        @Size(max = 100)
        private String cidade;

        @Size(max = 2)
        private String estado;

        @Size(max = 10)
        private String cep;

        @Size(max = 150)
        private String contato;

        private String observacao;
    }

    @Data
    public static class Response {
        private UUID id;
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
        private String email;
        private String telefone;
        private String endereco;
        private String cidade;
        private String estado;
        private String cep;
        private String contato;
        private String observacao;
        private boolean ativo;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
        private String telefone;
        private boolean ativo;
    }

    // =========================================================
    // COMPRA
    // =========================================================

    @Data
    public static class CompraCreateRequest {
        @NotNull(message = "Fornecedor é obrigatório")
        private UUID fornecedorId;

        @Size(max = 50)
        private String numeroNota;

        private LocalDateTime dtEntrega;

        private String observacao;

        @NotEmpty(message = "Pelo menos um item é obrigatório")
        @Valid
        private List<ItemCompraRequest> itens;

        // Se true, atualiza o preço de custo dos produtos ao receber
        private boolean atualizarPrecoCusto = true;
    }

    @Data
    public static class ItemCompraRequest {
        @NotNull(message = "Produto é obrigatório")
        private UUID produtoId;

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        private BigDecimal quantidade;

        @NotNull(message = "Preço de custo é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço de custo deve ser maior que zero")
        private BigDecimal precoCusto;
    }

    @Data
    public static class CompraResponse {
        private UUID id;
        private UUID tenantId;
        private FornecedorResumo fornecedor;
        private String numeroNota;
        private LocalDateTime dtCompra;
        private LocalDateTime dtEntrega;
        private BigDecimal total;
        private StatusCompra status;
        private String observacao;
        private String nomeUsuario;
        private List<ItemCompraResponse> itens;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class CompraResumoResponse {
        private UUID id;
        private FornecedorResumo fornecedor;
        private String numeroNota;
        private LocalDateTime dtCompra;
        private BigDecimal total;
        private StatusCompra status;
    }

    @Data
    public static class ItemCompraResponse {
        private UUID id;
        private UUID produtoId;
        private String nomeProduto;
        private BigDecimal quantidade;
        private BigDecimal precoCusto;
        private BigDecimal totalItem;
    }

    @Data
    public static class FornecedorResumo {
        private UUID id;
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
    }
}
