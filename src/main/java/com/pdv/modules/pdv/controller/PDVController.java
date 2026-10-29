package com.pdv.modules.pdv.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.pdv.dto.PDVDTOs;
import com.pdv.modules.pdv.service.PDVService;
import com.pdv.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pdvs")
@RequiredArgsConstructor
@Tag(name = "PDVs", description = "Gestão de dispositivos PDV")
public class PDVController {

    private final PDVService pdvService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista PDVs ativos do tenant — usado na tela de abrir caixa")
    public ResponseEntity<ApiResponse<List<PDVDTOs.Response>>> listar(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(pdvService.listarAtivos(principal.getTenantId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca PDV por ID")
    public ResponseEntity<ApiResponse<PDVDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(pdvService.buscarPorId(id, principal.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria um novo PDV")
    public ResponseEntity<ApiResponse<PDVDTOs.Response>> criar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PDVDTOs.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PDV criado com sucesso",
                        pdvService.criar(principal.getTenantId(), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza nome e descrição do PDV")
    public ResponseEntity<ApiResponse<PDVDTOs.Response>> atualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PDVDTOs.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("PDV atualizado com sucesso",
                        pdvService.atualizar(id, principal.getTenantId(), request)));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa um PDV")
    public ResponseEntity<ApiResponse<Void>> desativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        pdvService.desativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("PDV desativado com sucesso"));
    }
}