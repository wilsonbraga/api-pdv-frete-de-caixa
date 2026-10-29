package com.pdv.modules.venda.dto;

import com.pdv.modules.venda.entity.FormaPagamento;
import com.pdv.modules.venda.entity.StatusPagamento;
import com.pdv.modules.venda.entity.StatusVenda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class VendaDTOs {

    @Data
    public static class IniciarRequest {
        @NotNull(message = "Caixa é obrigatório")
        private UUID caixaId;

        private UUID clienteId;

        private String deviceId;
    }

    @Data
    public static class AdicionarItemRequest {
        @NotNull(message = "Produto é obrigatório")
        private UUID produtoId;

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        private BigDecimal quantidade;

        @DecimalMin(value = "0.0", message = "Desconto não pode ser negativo")
        private BigDecimal descontoItem = BigDecimal.ZERO;
    }

    @Data
    public static class AtualizarItemRequest {
        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001")
        private BigDecimal quantidade;

        @DecimalMin(value = "0.0")
        private BigDecimal descontoItem = BigDecimal.ZERO;
    }

    @Data
    public static class AplicarDescontoRequest {
        @NotNull(message = "Valor do desconto é obrigatório")
        @DecimalMin(value = "0.0", message = "Desconto não pode ser negativo")
        private BigDecimal desconto;

        // true = percentual, false = valor fixo
        private boolean percentual = false;
    }

    @Data
    public static class PagamentoRequest {
        @NotNull(message = "Forma de pagamento é obrigatória")
        private FormaPagamento forma;

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        private BigDecimal valor;

        private String nsu;
        private String autorizacao;
    }

    @Data
    public static class FinalizarRequest {
        @NotEmpty(message = "Pelo menos uma forma de pagamento é obrigatória")
        @Valid
        private List<PagamentoRequest> pagamentos;
    }

    @Data
    public static class CancelarRequest {
        @NotBlank(message = "Motivo de cancelamento é obrigatório")
        @Size(max = 500)
        private String motivo;
    }

    @Data
    public static class CancelarItemRequest {
        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 255)
        private String motivo;
    }

    // =====================================================================
    // RESPONSES
    // =====================================================================

    @Data
    public static class Response {
        private UUID id;
        private String numeroVenda;
        private UUID tenantId;
        private CaixaResumo caixa;
        private PDVResumo pdv;
        private OperadorResumo operador;
        private ClienteResumo cliente;
        private LocalDateTime dtVenda;
        private BigDecimal subtotal;
        private BigDecimal desconto;
        private BigDecimal total;
        private BigDecimal totalPago;
        private BigDecimal troco;
        private StatusVenda status;
        private String motivoCancelamento;
        private LocalDateTime dtCancelamento;
        private boolean sincronizado;
        private List<ItemVendaResponse> itens;
        private List<PagamentoResponse> pagamentos;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private String numeroVenda;
        private LocalDateTime dtVenda;
        private BigDecimal total;
        private StatusVenda status;
        private String nomeCliente;
        private String nomeOperador;
    }

    @Data
    public static class ItemVendaResponse {
        private UUID id;
        private UUID produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private String codigoProduto; // alias de codigoBarras para compatibilidade com o frontend
        private BigDecimal quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal descontoItem;
        private BigDecimal totalItem;
        private boolean cancelado;
    }

    @Data
    public static class PagamentoResponse {
        private UUID id;
        private FormaPagamento forma;
        private BigDecimal valor;
        private BigDecimal troco;
        private StatusPagamento status;
        private String nsu;
        private String autorizacao;
    }

    @Data
    public static class CaixaResumo {
        private UUID id;
    }

    @Data
    public static class PDVResumo {
        private UUID id;
        private String nome;
    }

    @Data
    public static class OperadorResumo {
        private UUID id;
        private String nome;
    }

    @Data
    public static class ClienteResumo {
        private UUID id;
        private String nome;
        private String cpfCnpj;
    }
}