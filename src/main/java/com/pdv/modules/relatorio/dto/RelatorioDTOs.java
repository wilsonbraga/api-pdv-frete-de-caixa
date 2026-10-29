package com.pdv.modules.relatorio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RelatorioDTOs {

    // =========================================================
    // DASHBOARD
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardResponse {
        private KPIs kpis;
        private List<VendasPorHora> vendasPorHora;
        private List<VendasPorDia> vendasUltimos7Dias;
        private List<FormaPagamentoBreakdown> breakdownPagamento;
        private List<ProdutoMaisVendido> topProdutos;
        private List<AlertaResponse> alertas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KPIs {
        private BigDecimal faturamentoHoje;
        private BigDecimal faturamentoMes;
        private long qtdVendasHoje;
        private BigDecimal ticketMedioHoje;
        private BigDecimal margemMediaHoje;
        private long produtosEstoqueCritico;
        private long caixasAbertos;
        private BigDecimal faturamentoOntem;
        private BigDecimal variacaoPercentual;
    }

    @Data
    @AllArgsConstructor
    public static class VendasPorHora {
        private int hora;
        private long qtdVendas;
        private BigDecimal total;
    }

    @Data
    @AllArgsConstructor
    public static class VendasPorDia {
        private LocalDate data;
        private long qtdVendas;
        private BigDecimal total;
    }

    @Data
    @AllArgsConstructor
    public static class FormaPagamentoBreakdown {
        private String forma;
        private BigDecimal total;
        private long quantidade;
        private BigDecimal percentual;
    }

    @Data
    @AllArgsConstructor
    public static class ProdutoMaisVendido {
        private UUID produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private BigDecimal quantidadeVendida;
        private BigDecimal totalVendido;
        private long qtdTransacoes;
    }

    @Data
    @AllArgsConstructor
    public static class AlertaResponse {
        private String tipo;   // ESTOQUE_CRITICO, CAIXA_NAO_FECHADO, FIADO_VENCIDO
        private String titulo;
        private String mensagem;
        private String nivel;  // INFO, AVISO, CRITICO
    }

    // =========================================================
    // RELATÓRIO DE VENDAS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatorioVendasResponse {
        private LocalDateTime inicio;
        private LocalDateTime fim;
        private long totalVendas;
        private long vendasCanceladas;
        private BigDecimal faturamentoBruto;
        private BigDecimal totalDescontos;
        private BigDecimal faturamentoLiquido;
        private BigDecimal ticketMedio;
        private Map<String, BigDecimal> totalPorFormaPagamento;
        private List<ItemRelatorioVenda> itens;
    }

    @Data
    @AllArgsConstructor
    public static class ItemRelatorioVenda {
        private String numeroVenda;
        private LocalDateTime dtVenda;
        private String nomeCliente;
        private String nomeOperador;
        private String nomePdv;
        private BigDecimal subtotal;
        private BigDecimal desconto;
        private BigDecimal total;
        private String status;
        private String formasPagamento;
    }

    // =========================================================
    // CURVA ABC
    // =========================================================

    @Data
    @AllArgsConstructor
    public static class CurvaAbcItem {
        private int posicao;
        private UUID produtoId;
        private String nomeProduto;
        private String categoria;
        private BigDecimal totalVendido;
        private BigDecimal percentualIndividual;
        private BigDecimal percentualAcumulado;
        private String classificacao; // A, B, C
    }

    // =========================================================
    // RELATÓRIO ESTOQUE
    // =========================================================

    @Data
    @AllArgsConstructor
    public static class RelatorioEstoqueItem {
        private UUID produtoId;
        private String nomeProduto;
        private String codigoBarras;
        private String categoria;
        private String unidade;
        private BigDecimal estoqueAtual;
        private BigDecimal estoqueMinimo;
        private BigDecimal precoCusto;
        private BigDecimal precoVenda;
        private BigDecimal valorEstoque;
        private boolean abaixoMinimo;
    }
}
