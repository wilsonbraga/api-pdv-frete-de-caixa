package com.pdv.modules.relatorio.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.relatorio.dto.RelatorioDTOs;
import com.pdv.modules.relatorio.repository.RelatorioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final RelatorioRepository relatorioRepository;
    private final ProdutoRepository produtoRepository;

    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =====================================================================
    // DASHBOARD
    // =====================================================================

    @Transactional(readOnly = true)
    public RelatorioDTOs.DashboardResponse dashboard(UUID tenantId) {
        LocalDateTime hoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime ontem = hoje.minusDays(1);
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // KPIs
        BigDecimal faturamentoHoje = relatorioRepository.sumFaturamento(tenantId, hoje, fimHoje);
        BigDecimal faturamentoOntem = relatorioRepository.sumFaturamento(tenantId, ontem, hoje.minusNanos(1));
        BigDecimal faturamentoMes = relatorioRepository.sumFaturamento(tenantId, inicioMes, fimHoje);
        long qtdVendasHoje = relatorioRepository.countVendas(tenantId, hoje, fimHoje);
        BigDecimal ticketMedioHoje = relatorioRepository.ticketMedio(tenantId, hoje, fimHoje);
        long caixasAbertos = relatorioRepository.countCaixasAbertos(tenantId);
        long produtosEstoqueCritico = relatorioRepository.countEstoqueCritico(tenantId);

        BigDecimal variacaoPercentual = calcularVariacao(faturamentoOntem, faturamentoHoje);

        RelatorioDTOs.KPIs kpis = RelatorioDTOs.KPIs.builder()
                .faturamentoHoje(faturamentoHoje)
                .faturamentoMes(faturamentoMes)
                .qtdVendasHoje(qtdVendasHoje)
                .ticketMedioHoje(ticketMedioHoje)
                .margemMediaHoje(BigDecimal.ZERO) // calculado se necessário
                .produtosEstoqueCritico(produtosEstoqueCritico)
                .caixasAbertos(caixasAbertos)
                .faturamentoOntem(faturamentoOntem)
                .variacaoPercentual(variacaoPercentual)
                .build();

        // Vendas por hora
        List<RelatorioDTOs.VendasPorHora> porHora = relatorioRepository
                .vendasPorHora(tenantId, hoje, fimHoje);

        // Últimos 7 dias
        List<RelatorioDTOs.VendasPorDia> ultimos7Dias = relatorioRepository
                .vendasPorDia(tenantId, hoje.minusDays(6), fimHoje);

        // Breakdown pagamento (hoje)
        List<RelatorioDTOs.FormaPagamentoBreakdown> breakdown = relatorioRepository
                .breakdownPagamento(tenantId, hoje, fimHoje);

        // Top 10 produtos (últimos 30 dias)
        List<RelatorioDTOs.ProdutoMaisVendido> topProdutos = relatorioRepository
                .topProdutos(tenantId, hoje.minusDays(30), fimHoje, 10);

        // Alertas
        List<RelatorioDTOs.AlertaResponse> alertas = gerarAlertas(
                tenantId, produtosEstoqueCritico, caixasAbertos);

        return RelatorioDTOs.DashboardResponse.builder()
                .kpis(kpis)
                .vendasPorHora(porHora)
                .vendasUltimos7Dias(ultimos7Dias)
                .breakdownPagamento(breakdown)
                .topProdutos(topProdutos)
                .alertas(alertas)
                .build();
    }

    // =====================================================================
    // RELATÓRIO DE VENDAS
    // =====================================================================

    @Transactional(readOnly = true)
    public RelatorioDTOs.RelatorioVendasResponse relatorioVendas(UUID tenantId,
                                                                   LocalDateTime inicio,
                                                                   LocalDateTime fim,
                                                                   UUID operadorId,
                                                                   UUID pdvId,
                                                                   String status) {
        List<RelatorioDTOs.ItemRelatorioVenda> itens = relatorioRepository
                .relatorioVendasDetalhado(tenantId, inicio, fim, operadorId, pdvId, status);

        BigDecimal faturamentoBruto = itens.stream()
                .filter(i -> "FINALIZADA".equals(i.getStatus()))
                .map(RelatorioDTOs.ItemRelatorioVenda::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDescontos = itens.stream()
                .filter(i -> "FINALIZADA".equals(i.getStatus()))
                .map(RelatorioDTOs.ItemRelatorioVenda::getDesconto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalVendas = itens.stream()
                .filter(i -> "FINALIZADA".equals(i.getStatus())).count();

        long canceladas = itens.stream()
                .filter(i -> "CANCELADA".equals(i.getStatus())).count();

        BigDecimal ticketMedio = totalVendas > 0
                ? faturamentoBruto.divide(BigDecimal.valueOf(totalVendas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<RelatorioDTOs.FormaPagamentoBreakdown> breakdown = relatorioRepository
                .breakdownPagamento(tenantId, inicio, fim);

        java.util.Map<String, BigDecimal> porForma = new java.util.LinkedHashMap<>();
        breakdown.forEach(b -> porForma.put(b.getForma(), b.getTotal()));

        return RelatorioDTOs.RelatorioVendasResponse.builder()
                .inicio(inicio)
                .fim(fim)
                .totalVendas(totalVendas)
                .vendasCanceladas(canceladas)
                .faturamentoBruto(faturamentoBruto)
                .totalDescontos(totalDescontos)
                .faturamentoLiquido(faturamentoBruto.subtract(totalDescontos))
                .ticketMedio(ticketMedio)
                .totalPorFormaPagamento(porForma)
                .itens(itens)
                .build();
    }

    // =====================================================================
    // CURVA ABC
    // =====================================================================

    @Transactional(readOnly = true)
    public List<RelatorioDTOs.CurvaAbcItem> curvaAbc(UUID tenantId,
                                                       LocalDateTime inicio,
                                                       LocalDateTime fim) {
        List<Object[]> raw = relatorioRepository.curvaAbcRaw(tenantId, inicio, fim);

        BigDecimal totalGeral = raw.stream()
                .map(r -> new BigDecimal(r[3].toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RelatorioDTOs.CurvaAbcItem> result = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (int i = 0; i < raw.size(); i++) {
            Object[] row = raw.get(i);
            BigDecimal totalItem = new BigDecimal(row[3].toString());

            BigDecimal percentualIndividual = totalGeral.compareTo(BigDecimal.ZERO) > 0
                    ? totalItem.divide(totalGeral, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            acumulado = acumulado.add(percentualIndividual);

            String classificacao;
            if (acumulado.compareTo(BigDecimal.valueOf(80)) <= 0) {
                classificacao = "A";
            } else if (acumulado.compareTo(BigDecimal.valueOf(95)) <= 0) {
                classificacao = "B";
            } else {
                classificacao = "C";
            }

            result.add(new RelatorioDTOs.CurvaAbcItem(
                    i + 1,
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    totalItem,
                    percentualIndividual,
                    acumulado.setScale(2, RoundingMode.HALF_UP),
                    classificacao
            ));
        }
        return result;
    }

    // =====================================================================
    // RELATÓRIO DE ESTOQUE
    // =====================================================================

    @Transactional(readOnly = true)
    public List<RelatorioDTOs.RelatorioEstoqueItem> relatorioEstoque(UUID tenantId,
                                                                       UUID categoriaId,
                                                                       Boolean apenasAbaixoMinimo) {
        return relatorioRepository.relatorioEstoque(tenantId, categoriaId, apenasAbaixoMinimo);
    }

    // =====================================================================
    // EXPORTAÇÃO EXCEL
    // =====================================================================

    public byte[] exportarVendasExcel(RelatorioDTOs.RelatorioVendasResponse relatorio)
            throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Vendas");

            // Estilos
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle moedaStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            moedaStyle.setDataFormat(df.getFormat("R$ #,##0.00"));

            // Cabeçalho resumo
            Row resumoRow = sheet.createRow(0);
            criarCelula(resumoRow, 0, "Período:", headerStyle);
            criarCelula(resumoRow, 1,
                    relatorio.getInicio().format(FMT_DT) + " a " + relatorio.getFim().format(FMT_DT),
                    null);

            Row faturRow = sheet.createRow(1);
            criarCelula(faturRow, 0, "Faturamento:", headerStyle);
            criarCelulaNumerica(faturRow, 1, relatorio.getFaturamentoBruto(), moedaStyle);

            Row ticketRow = sheet.createRow(2);
            criarCelula(ticketRow, 0, "Ticket Médio:", headerStyle);
            criarCelulaNumerica(ticketRow, 1, relatorio.getTicketMedio(), moedaStyle);

            // Linha em branco
            sheet.createRow(3);

            // Header das colunas
            Row header = sheet.createRow(4);
            String[] colunas = {"Nº Venda", "Data/Hora", "Cliente", "Operador", "PDV",
                    "Subtotal", "Desconto", "Total", "Status"};
            for (int i = 0; i < colunas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            int rowIdx = 5;
            for (RelatorioDTOs.ItemRelatorioVenda item : relatorio.getItens()) {
                Row row = sheet.createRow(rowIdx++);
                criarCelula(row, 0, item.getNumeroVenda(), null);
                criarCelula(row, 1, item.getDtVenda().format(FMT_DT), null);
                criarCelula(row, 2, item.getNomeCliente(), null);
                criarCelula(row, 3, item.getNomeOperador(), null);
                criarCelula(row, 4, item.getNomePdv(), null);
                criarCelulaNumerica(row, 5, item.getSubtotal(), moedaStyle);
                criarCelulaNumerica(row, 6, item.getDesconto(), moedaStyle);
                criarCelulaNumerica(row, 7, item.getTotal(), moedaStyle);
                criarCelula(row, 8, item.getStatus(), null);
            }

            // Autosize
            for (int i = 0; i < colunas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarEstoqueExcel(List<RelatorioDTOs.RelatorioEstoqueItem> itens)
            throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Estoque");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle moedaStyle = wb.createCellStyle();
            DataFormat df = wb.createDataFormat();
            moedaStyle.setDataFormat(df.getFormat("R$ #,##0.00"));

            CellStyle alertStyle = wb.createCellStyle();
            alertStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            alertStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            String[] colunas = {"Produto", "Cód. Barras", "Categoria", "Unidade",
                    "Estoque Atual", "Estoque Mínimo", "Preço Custo", "Preço Venda",
                    "Valor Estoque", "Situação"};
            for (int i = 0; i < colunas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (RelatorioDTOs.RelatorioEstoqueItem item : itens) {
                Row row = sheet.createRow(rowIdx++);
                criarCelula(row, 0, item.getNomeProduto(), null);
                criarCelula(row, 1, item.getCodigoBarras(), null);
                criarCelula(row, 2, item.getCategoria(), null);
                criarCelula(row, 3, item.getUnidade(), null);
                criarCelulaNumerica(row, 4, item.getEstoqueAtual(), null);
                criarCelulaNumerica(row, 5, item.getEstoqueMinimo(), null);
                criarCelulaNumerica(row, 6, item.getPrecoCusto(), moedaStyle);
                criarCelulaNumerica(row, 7, item.getPrecoVenda(), moedaStyle);
                criarCelulaNumerica(row, 8, item.getValorEstoque(), moedaStyle);

                String situacao = item.isAbaixoMinimo() ? "⚠ ABAIXO DO MÍNIMO" : "OK";
                Cell situacaoCell = row.createCell(9);
                situacaoCell.setCellValue(situacao);
                if (item.isAbaixoMinimo()) {
                    situacaoCell.setCellStyle(alertStyle);
                }
            }

            for (int i = 0; i < colunas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarCurvaAbcExcel(List<RelatorioDTOs.CurvaAbcItem> itens)
            throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Curva ABC");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            DataFormat df = wb.createDataFormat();
            CellStyle moedaStyle = wb.createCellStyle();
            moedaStyle.setDataFormat(df.getFormat("R$ #,##0.00"));
            CellStyle pctStyle = wb.createCellStyle();
            pctStyle.setDataFormat(df.getFormat("0.00%"));

            Map<String, Short> classColors = Map.of(
                    "A", IndexedColors.LIGHT_GREEN.getIndex(),
                    "B", IndexedColors.LIGHT_YELLOW.getIndex(),
                    "C", IndexedColors.ROSE.getIndex()
            );

            Row header = sheet.createRow(0);
            String[] colunas = {"Posição", "Produto", "Categoria", "Total Vendido",
                    "% Individual", "% Acumulado", "Classificação"};
            for (int i = 0; i < colunas.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(colunas[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (RelatorioDTOs.CurvaAbcItem item : itens) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getPosicao());
                criarCelula(row, 1, item.getNomeProduto(), null);
                criarCelula(row, 2, item.getCategoria(), null);
                criarCelulaNumerica(row, 3, item.getTotalVendido(), moedaStyle);
                criarCelulaNumerica(row, 4,
                        item.getPercentualIndividual().divide(BigDecimal.valueOf(100), 4,
                                RoundingMode.HALF_UP), pctStyle);
                criarCelulaNumerica(row, 5,
                        item.getPercentualAcumulado().divide(BigDecimal.valueOf(100), 4,
                                RoundingMode.HALF_UP), pctStyle);

                Cell classCell = row.createCell(6);
                classCell.setCellValue(item.getClassificacao());
                CellStyle cs = wb.createCellStyle();
                cs.setFillForegroundColor(classColors.getOrDefault(
                        item.getClassificacao(), IndexedColors.WHITE.getIndex()));
                cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                classCell.setCellStyle(cs);
            }

            for (int i = 0; i < colunas.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }

    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private BigDecimal calcularVariacao(BigDecimal anterior, BigDecimal atual) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return atual.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return atual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<RelatorioDTOs.AlertaResponse> gerarAlertas(UUID tenantId,
                                                              long estoqueCritico,
                                                              long caixasAbertos) {
        List<RelatorioDTOs.AlertaResponse> alertas = new ArrayList<>();

        if (estoqueCritico > 0) {
            alertas.add(new RelatorioDTOs.AlertaResponse(
                    "ESTOQUE_CRITICO",
                    "Estoque Crítico",
                    estoqueCritico + " produto(s) abaixo do estoque mínimo",
                    estoqueCritico > 5 ? "CRITICO" : "AVISO"
            ));
        }

        if (caixasAbertos == 0) {
            alertas.add(new RelatorioDTOs.AlertaResponse(
                    "SEM_CAIXA_ABERTO",
                    "Nenhum Caixa Aberto",
                    "Não há caixas abertos no momento",
                    "INFO"
            ));
        }

        return alertas;
    }

    private void criarCelula(Row row, int col, String valor, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void criarCelulaNumerica(Row row, int col, BigDecimal valor, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor.doubleValue() : 0.0);
        if (style != null) cell.setCellStyle(style);
    }
}
