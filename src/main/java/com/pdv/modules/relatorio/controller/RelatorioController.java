package com.pdv.modules.relatorio.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.relatorio.dto.RelatorioDTOs;
import com.pdv.modules.relatorio.service.RelatorioService;
import com.pdv.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'FINANCEIRO')")
@Tag(name = "Relatórios", description = "Dashboard, relatórios e exportações")
public class RelatorioController {

    private final RelatorioService relatorioService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // =====================================================================
    // DASHBOARD
    // =====================================================================

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard principal com KPIs, gráficos e alertas em tempo real")
    public ResponseEntity<ApiResponse<RelatorioDTOs.DashboardResponse>> dashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(relatorioService.dashboard(principal.getTenantId())));
    }

    // =====================================================================
    // VENDAS
    // =====================================================================

    @GetMapping("/vendas")
    @Operation(summary = "Relatório de vendas por período com totalizações")
    public ResponseEntity<ApiResponse<RelatorioDTOs.RelatorioVendasResponse>> relatorioVendas(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID operadorId,
            @RequestParam(required = false) UUID pdvId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                relatorioService.relatorioVendas(
                        principal.getTenantId(),
                        inicio.atStartOfDay(),
                        fim.atTime(LocalTime.MAX),
                        operadorId, pdvId, status)));
    }

    @GetMapping("/vendas/export/excel")
    @Operation(summary = "Exporta relatório de vendas em Excel (.xlsx)")
    public ResponseEntity<byte[]> exportarVendasExcel(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID operadorId,
            @RequestParam(required = false) UUID pdvId,
            @RequestParam(required = false) String status) throws Exception {

        RelatorioDTOs.RelatorioVendasResponse relatorio = relatorioService.relatorioVendas(
                principal.getTenantId(),
                inicio.atStartOfDay(),
                fim.atTime(LocalTime.MAX),
                operadorId, pdvId, status);

        byte[] bytes = relatorioService.exportarVendasExcel(relatorio);
        String filename = "vendas_" + inicio.format(FMT) + "_" + fim.format(FMT) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // =====================================================================
    // CURVA ABC
    // =====================================================================

    @GetMapping("/curva-abc")
    @Operation(summary = "Curva ABC de produtos por valor de venda no período")
    public ResponseEntity<ApiResponse<List<RelatorioDTOs.CurvaAbcItem>>> curvaAbc(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(ApiResponse.success(
                relatorioService.curvaAbc(
                        principal.getTenantId(),
                        inicio.atStartOfDay(),
                        fim.atTime(LocalTime.MAX))));
    }

    @GetMapping("/curva-abc/export/excel")
    @Operation(summary = "Exporta curva ABC em Excel")
    public ResponseEntity<byte[]> exportarCurvaAbcExcel(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) throws Exception {

        List<RelatorioDTOs.CurvaAbcItem> itens = relatorioService.curvaAbc(
                principal.getTenantId(),
                inicio.atStartOfDay(),
                fim.atTime(LocalTime.MAX));

        byte[] bytes = relatorioService.exportarCurvaAbcExcel(itens);
        String filename = "curva_abc_" + inicio.format(FMT) + "_" + fim.format(FMT) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // =====================================================================
    // ESTOQUE
    // =====================================================================

    @GetMapping("/estoque")
    @Operation(summary = "Relatório de estoque atual com valores")
    public ResponseEntity<ApiResponse<List<RelatorioDTOs.RelatorioEstoqueItem>>> relatorioEstoque(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Boolean apenasAbaixoMinimo) {
        return ResponseEntity.ok(ApiResponse.success(
                relatorioService.relatorioEstoque(
                        principal.getTenantId(), categoriaId, apenasAbaixoMinimo)));
    }

    @GetMapping("/estoque/export/excel")
    @Operation(summary = "Exporta relatório de estoque em Excel")
    public ResponseEntity<byte[]> exportarEstoqueExcel(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Boolean apenasAbaixoMinimo) throws Exception {

        List<RelatorioDTOs.RelatorioEstoqueItem> itens = relatorioService.relatorioEstoque(
                principal.getTenantId(), categoriaId, apenasAbaixoMinimo);

        byte[] bytes = relatorioService.exportarEstoqueExcel(itens);
        String filename = "estoque_" + LocalDate.now().format(FMT) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
