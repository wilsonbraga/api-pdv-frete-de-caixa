package com.pdv.modules.cliente.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ClienteDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        private String nome;

        @Size(max = 18)
        private String cpfCnpj;

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

        private LocalDate dataNascimento;

        @DecimalMin(value = "0.0", message = "Limite de crédito não pode ser negativo")
        private BigDecimal limiteCredito = BigDecimal.ZERO;

        private String observacao;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        private String nome;

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

        private LocalDate dataNascimento;

        @DecimalMin(value = "0.0")
        private BigDecimal limiteCredito;

        private String observacao;
    }

    @Data
    public static class Response {
        private UUID id;
        private String nome;
        private String cpfCnpj;
        private String email;
        private String telefone;
        private String endereco;
        private String cidade;
        private String estado;
        private String cep;
        private LocalDate dataNascimento;
        private BigDecimal limiteCredito;
        private BigDecimal saldoDevedor;
        private BigDecimal creditoDisponivel;
        private String observacao;
        private boolean ativo;
        private LocalDateTime criadoEm;
        private LocalDateTime atualizadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private String nome;
        private String cpfCnpj;
        private String telefone;
        private BigDecimal limiteCredito;
        private BigDecimal saldoDevedor;
        private boolean ativo;
    }

    @Data
    public static class AtualizarSaldoRequest {
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        private BigDecimal valor;

        @NotBlank(message = "Descrição é obrigatória")
        private String descricao;
    }
}
