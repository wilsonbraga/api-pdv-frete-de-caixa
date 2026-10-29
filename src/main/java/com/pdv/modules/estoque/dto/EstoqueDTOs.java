package com.pdv.modules.estoque.dto;

import com.pdv.modules.estoque.entity.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class EstoqueDTOs {

    @Data
    public static class AjusteRequest {
        @NotNull(message = "Produto é obrigatório")
        private UUID produtoId;

        @NotNull(message = "Tipo de movimentação é obrigatório")
        private TipoMovimentacaoEstoque tipo;

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        private BigDecimal quantidade;

        @NotBlank(message = "Motivo é obrigatório para ajuste manual")
        @Size(max = 255)
        private String motivo;
    }

    @Data
    public static class MovimentacaoResponse {
        private UUID id;
        private UUID produtoId;
        private String nomeProduto;
        private TipoMovimentacaoEstoque tipo;
        private BigDecimal quantidade;
        private BigDecimal quantidadeAnterior;
        private BigDecimal quantidadePosterior;
        private String motivo;
        private UUID referenciaId;
        private String referenciaTipo;
        private String nomeUsuario;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class EstoqueAtualResponse {
        private UUID produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private String unidade;
        private BigDecimal estoqueAtual;
        private BigDecimal estoqueMinimo;
        private boolean abaixoMinimo;
        private String nomeCategoria;
    }

    @Data
    public static class InventarioItemRequest {
        @NotNull(message = "Produto é obrigatório")
        private UUID produtoId;

        @NotNull(message = "Quantidade contada é obrigatória")
        @DecimalMin(value = "0.0")
        private BigDecimal quantidadeContada;
    }
}
