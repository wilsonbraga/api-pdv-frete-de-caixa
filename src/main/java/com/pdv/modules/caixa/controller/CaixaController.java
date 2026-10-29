package com.pdv.modules.caixa.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.caixa.dto.CaixaDTOs;
import com.pdv.modules.caixa.entity.StatusCaixa;
import com.pdv.modules.caixa.service.CaixaService;
import com.pdv.shared.dto.ApiResponse;
import com.pdv.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/caixas")
@RequiredArgsConstructor
@Tag(name = "Caixa", description = "Abertura, fechamento, sangria e suprimento de caixa")
public class CaixaController {

    private final CaixaService caixaService;

    @PostMapping("/abrir")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Abre um novo caixa para o PDV informado")
    public ResponseEntity<ApiResponse<CaixaDTOs.Response>> abrir(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CaixaDTOs.AbrirRequest request) {
        CaixaDTOs.Response response = caixaService.abrir(
                principal.getTenantId(), principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Caixa aberto com sucesso", response));
    }

    @PatchMapping("/{id}/fechar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fecha o caixa informado")
    public ResponseEntity<ApiResponse<CaixaDTOs.Response>> fechar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) CaixaDTOs.FecharRequest request) {
        CaixaDTOs.FecharRequest req = request != null ? request : new CaixaDTOs.FecharRequest();
        CaixaDTOs.Response response = caixaService.fechar(
                id, principal.getTenantId(), principal.getId(), req);
        return ResponseEntity.ok(ApiResponse.success("Caixa fechado com sucesso", response));
    }

    @PostMapping("/{id}/sangria")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Realiza sangria (retirada de dinheiro) do caixa")
    public ResponseEntity<ApiResponse<CaixaDTOs.MovimentacaoResponse>> sangria(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CaixaDTOs.SangriaSuprimentoRequest request) {
        CaixaDTOs.MovimentacaoResponse response = caixaService.sangria(
                id, principal.getTenantId(), principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Sangria realizada com sucesso", response));
    }

    @PostMapping("/{id}/suprimento")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Realiza suprimento (reforço de dinheiro) no caixa")
    public ResponseEntity<ApiResponse<CaixaDTOs.MovimentacaoResponse>> suprimento(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CaixaDTOs.SangriaSuprimentoRequest request) {
        CaixaDTOs.MovimentacaoResponse response = caixaService.suprimento(
                id, principal.getTenantId(), principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Suprimento realizado com sucesso", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca caixa por ID")
    public ResponseEntity<ApiResponse<CaixaDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                caixaService.buscarPorId(id, principal.getTenantId())));
    }

    @GetMapping("/aberto/pdv/{pdvId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca o caixa aberto de um PDV específico. Retorna 204 se não houver caixa aberto.")
    public ResponseEntity<ApiResponse<CaixaDTOs.Response>> buscarCaixaAbertoPorPdv(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID pdvId) {
        return caixaService.buscarCaixaAbertoPorPdv(pdvId, principal.getTenantId())
                .map(caixa -> ResponseEntity.ok(ApiResponse.success(caixa)))
                .orElse(ResponseEntity.noContent().build()); // 204 = caixa fechado, estado normal
    }

    @GetMapping("/meu-caixa")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retorna o caixa aberto do operador logado. Retorna 204 se não houver.")
    public ResponseEntity<ApiResponse<CaixaDTOs.Response>> meuCaixaAberto(
            @AuthenticationPrincipal UserPrincipal principal) {
        return caixaService.buscarMeuCaixaAberto(principal.getId(), principal.getTenantId())
                .map(caixa -> ResponseEntity.ok(ApiResponse.success(caixa)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/abertos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista todos os caixas abertos do tenant — visão do gerente")
    public ResponseEntity<ApiResponse<List<CaixaDTOs.ResumoResponse>>> listarAbertos(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                caixaService.listarCaixasAbertos(principal.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista caixas com filtros e paginação")
    public ResponseEntity<ApiResponse<PageResponse<CaixaDTOs.ResumoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID pdvId,
            @RequestParam(required = false) UUID operadorId,
            @RequestParam(required = false) StatusCaixa status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dtAbertura"));
        return ResponseEntity.ok(ApiResponse.success(
                caixaService.listar(principal.getTenantId(), pdvId, operadorId,
                        status, inicio, fim, pageable)));
    }
}