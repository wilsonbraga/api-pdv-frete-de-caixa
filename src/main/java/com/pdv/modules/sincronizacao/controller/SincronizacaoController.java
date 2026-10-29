package com.pdv.modules.sincronizacao.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.sincronizacao.dto.SincronizacaoDTOs;
import com.pdv.modules.sincronizacao.service.SincronizacaoService;
import com.pdv.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
@Tag(name = "Sincronização", description = "Endpoints para sincronização offline-first")
public class SincronizacaoController {

    private final SincronizacaoService sincronizacaoService;

    @PostMapping("/lote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Envia lote de operações offline para sincronização")
    public ResponseEntity<ApiResponse<SincronizacaoDTOs.LoteSyncResponse>> sincronizarLote(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SincronizacaoDTOs.LoteSyncRequest request) {
        SincronizacaoDTOs.LoteSyncResponse response = sincronizacaoService.receberLote(
                principal.getTenantId(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "Lote recebido: " + response.getTotalSincronizados() + " enfileirado(s)",
                response));
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulta status de sincronização do tenant")
    public ResponseEntity<ApiResponse<SincronizacaoDTOs.StatusSyncResponse>> status(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                sincronizacaoService.getStatus(principal.getTenantId())));
    }
}
