package com.pdv.modules.tenant.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.tenant.dto.TenantDTOs;
import com.pdv.modules.tenant.service.TenantService;
import com.pdv.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller separado para o endpoint /tenant/info (singular, sem 's').
 * Usado pelo cliente JavaFX (PdvController.buscarTenant) para obter os dados
 * do tenant do operador logado — necessário para geração do cupom PDF.
 *
 * O TenantController principal está mapeado em /tenants (plural) e exige ADMIN.
 * Este endpoint é acessível a qualquer usuário autenticado.
 */
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant Info", description = "Informações do tenant do operador logado")
public class TenantInfoController {

    private final TenantService tenantService;

    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retorna dados do tenant do operador logado (para cupom e uso geral do frontend)")
    public ResponseEntity<ApiResponse<TenantDTOs.Response>> info(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(tenantService.buscarPorId(principal.getTenantId())));
    }
}