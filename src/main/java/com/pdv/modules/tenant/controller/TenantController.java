package com.pdv.modules.tenant.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.tenant.dto.TenantDTOs;
import com.pdv.modules.tenant.service.TenantService;
import com.pdv.shared.dto.ApiResponse;
import com.pdv.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Gestão de empresas (multi-tenant)")
public class TenantController {
	
	//SÓ O DONO DO SISTEMA PODERÁ CRIAR tenant. TEM QUE TER UMA AREA PARA ADMISTRAR OS CLIENTES / USUARIOS DO SISTEAMA

    private final TenantService tenantService;

    /**
     * Endpoint público (para qualquer usuário autenticado) que retorna os dados
     * do tenant do operador logado. Usado pelo frontend para imprimir cupons etc.
     * Mapeado em /tenant/info (sem 's') para compatibilidade com o cliente JavaFX.
     */
    @GetMapping(value = "/info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retorna dados do tenant do operador logado")
    public ResponseEntity<ApiResponse<TenantDTOs.Response>> info(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.buscarPorId(principal.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria um novo tenant")
    public ResponseEntity<ApiResponse<TenantDTOs.Response>> criar(
            @Valid @RequestBody TenantDTOs.CreateRequest request) {
        TenantDTOs.Response response = tenantService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tenant criado com sucesso", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Busca tenant por ID")
    public ResponseEntity<ApiResponse<TenantDTOs.Response>> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(tenantService.buscarPorId(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista tenants com filtros e paginação")
    public ResponseEntity<ApiResponse<PageResponse<TenantDTOs.ResumoResponse>>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nome") String sort) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(ApiResponse.success(tenantService.listar(nome, ativo, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza dados do tenant")
    public ResponseEntity<ApiResponse<TenantDTOs.Response>> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TenantDTOs.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Tenant atualizado com sucesso", tenantService.atualizar(id, request)));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativa um tenant")
    public ResponseEntity<ApiResponse<Void>> ativar(@PathVariable UUID id) {
        tenantService.ativar(id);
        return ResponseEntity.ok(ApiResponse.success("Tenant ativado com sucesso"));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa um tenant")
    public ResponseEntity<ApiResponse<Void>> desativar(@PathVariable UUID id) {
        tenantService.desativar(id);
        return ResponseEntity.ok(ApiResponse.success("Tenant desativado com sucesso"));
    }
}