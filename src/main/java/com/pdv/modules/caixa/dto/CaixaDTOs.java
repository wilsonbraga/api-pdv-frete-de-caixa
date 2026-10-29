package com.pdv.modules.caixa.dto;

import com.pdv.modules.caixa.entity.StatusCaixa;
import com.pdv.modules.caixa.entity.TipoMovimentacaoCaixa;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CaixaDTOs {

    @Data
    public static class AbrirRequest {
        @NotNull(message = "PDV é obrigatório")
        private UUID pdvId;

        @NotNull(message = "Valor inicial é obrigatório")
        @DecimalMin(value = "0.0", message = "Valor inicial não pode ser negativo")
        private BigDecimal valorInicial;
    }

    @Data
    public static class FecharRequest {
        private String observacao;
    }

    @Data
    public static class SangriaSuprimentoRequest {
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        private BigDecimal valor;

        @NotBlank(message = "Motivo é obrigatório")
        @Size(max = 255)
        private String motivo;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID tenantId;
        private PDVResumo pdv;
        private OperadorResumo operador;
        private LocalDateTime dtAbertura;
        private LocalDateTime dtFechamento;
        private BigDecimal valorInicial;
        private BigDecimal totalDinheiro;
        private BigDecimal totalDebito;
        private BigDecimal totalCredito;
        private BigDecimal totalPix;
        private BigDecimal totalFiado;
        private BigDecimal totalSangria;
        private BigDecimal totalSuprimento;
        private BigDecimal totalVendas;
        private BigDecimal saldoDinheiro;
        private StatusCaixa status;
        private String observacaoFechamento;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class ResumoResponse {
        private UUID id;
        private PDVResumo pdv;
        private OperadorResumo operador;
        private LocalDateTime dtAbertura;
        private LocalDateTime dtFechamento;
        private BigDecimal totalVendas;
        private BigDecimal saldoDinheiro;
        private StatusCaixa status;
    }

    @Data
    public static class MovimentacaoResponse {
        private UUID id;
        private TipoMovimentacaoCaixa tipo;
        private BigDecimal valor;
        private String motivo;
        private String nomeOperador;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class PDVResumo {
        private UUID id;
        private String nome;
        private String deviceId;
    }

    @Data
    public static class OperadorResumo {
        private UUID id;
        private String nome;
    }
}
