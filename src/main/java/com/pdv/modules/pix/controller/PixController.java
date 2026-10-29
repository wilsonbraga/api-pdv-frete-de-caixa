package com.pdv.modules.pix.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.pix.dto.PixDTOs;
import com.pdv.modules.pix.service.PixService;
import com.pdv.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("/pix")
@RequiredArgsConstructor
@Tag(name = "PIX", description = "Geração de cobranças PIX e configuração do Mercado Pago")
public class PixController {

    private final PixService pixService;

    // =====================================================================
    // CONFIGURAÇÃO
    // =====================================================================

    @PostMapping("/configuracoes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra credenciais do Mercado Pago para o tenant")
    public ResponseEntity<ApiResponse<PixDTOs.ConfiguracaoResponse>> salvarConfiguracao(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PixDTOs.ConfiguracaoCreateRequest request) {
        PixDTOs.ConfiguracaoResponse response = pixService.salvarConfiguracao(
                principal.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Configuração PIX cadastrada com sucesso", response));
    }

    @PutMapping("/configuracoes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza credenciais do Mercado Pago")
    public ResponseEntity<ApiResponse<PixDTOs.ConfiguracaoResponse>> atualizarConfiguracao(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody PixDTOs.ConfiguracaoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Configuração atualizada com sucesso",
                pixService.atualizarConfiguracao(id, principal.getTenantId(), request)));
    }

    @GetMapping("/configuracoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista configurações PIX ativas do tenant")
    public ResponseEntity<ApiResponse<List<PixDTOs.ConfiguracaoResponse>>> listarConfiguracoes(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                pixService.listarConfiguracoes(principal.getTenantId())));
    }

    // =====================================================================
    // COBRANÇA PIX
    // =====================================================================

    @PostMapping("/cobrar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gera uma cobrança PIX — retorna QR Code para exibir no PDV")
    public ResponseEntity<ApiResponse<PixDTOs.CobrancaResponse>> gerarCobranca(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PixDTOs.GerarCobrancaRequest request) {
        PixDTOs.CobrancaResponse response = pixService.gerarCobranca(
                principal.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cobrança PIX gerada com sucesso", response));
    }

    @GetMapping("/cobrar/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulta status da cobrança PIX — usado para polling no PDV")
    public ResponseEntity<ApiResponse<PixDTOs.StatusCobrancaResponse>> consultarStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                pixService.consultarStatus(id, principal.getTenantId())));
    }

    @GetMapping("/cobrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista cobranças PIX com filtros e paginação — para o histórico no painel")
    public ResponseEntity<ApiResponse<Page<PixDTOs.CobrancaResponse>>> listarCobrancas(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PixDTOs.CobrancaFiltroRequest filtro = new PixDTOs.CobrancaFiltroRequest();

        if (status != null && !status.isBlank()) {
            filtro.setStatus(
                com.pdv.modules.pix.entity.StatusCobrancaPix.valueOf(status.toUpperCase())
            );
        }
        if (dataInicio != null && !dataInicio.isBlank()) {
            filtro.setDataInicio(java.time.LocalDate.parse(dataInicio));
        }
        if (dataFim != null && !dataFim.isBlank()) {
            filtro.setDataFim(java.time.LocalDate.parse(dataFim));
        }

        Page<PixDTOs.CobrancaResponse> resultado =
                pixService.listarCobrancas(principal.getTenantId(), filtro, page, size);

        return ResponseEntity.ok(ApiResponse.success(resultado));
    }

    // =====================================================================
    // WEBHOOK MERCADO PAGO
    // Endpoint público — o MP chama este endpoint quando o pagamento é confirmado
    // URL configurada no painel MP: https://api.seudominio.com.br/api/pix/webhook/mercadopago/{tenantId}
    // =====================================================================

    @PostMapping("/webhook/mercadopago/{tenantId}")
    @Operation(summary = "Webhook Mercado Pago — recebe confirmação de pagamento PIX")
    public ResponseEntity<Void> webhookMercadoPago(
            @PathVariable UUID tenantId,
            @RequestBody PixDTOs.MpWebhookRequest webhook,
            HttpServletRequest httpRequest) {

        // Captura o payload raw para salvar no banco (auditoria)
        String payloadRaw;
        try {
            payloadRaw = new String(
                    httpRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            payloadRaw = "{}";
        }

        log.info("Webhook MP recebido — tenantId: {} | action: {}",
                tenantId, webhook.getAction());

        // Processa de forma assíncrona para responder 200 imediatamente ao MP
        // O MP considera falha se não receber 200 em < 5 segundos
        pixService.processarWebhookMercadoPago(tenantId, webhook, payloadRaw);

        // MP exige 200 OK — qualquer outro status causa reenvio
        return ResponseEntity.ok().build();
    }
}
