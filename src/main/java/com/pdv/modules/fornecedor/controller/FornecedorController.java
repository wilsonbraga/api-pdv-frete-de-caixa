package com.pdv.modules.fornecedor.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.fornecedor.dto.FornecedorDTOs;
import com.pdv.modules.fornecedor.entity.StatusCompra;
import com.pdv.modules.fornecedor.service.FornecedorService;
import com.pdv.shared.dto.ApiResponse;
import com.pdv.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/fornecedores")
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores e lançamento de compras")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    // =====================================================================
    // FORNECEDOR
    // =====================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cria um novo fornecedor")
    public ResponseEntity<ApiResponse<FornecedorDTOs.Response>> criar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FornecedorDTOs.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Fornecedor criado com sucesso",
                        fornecedorService.criar(principal.getTenantId(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'FINANCEIRO')")
    @Operation(summary = "Busca fornecedor por ID")
    public ResponseEntity<ApiResponse<FornecedorDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                fornecedorService.buscarPorId(id, principal.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'FINANCEIRO')")
    @Operation(summary = "Lista fornecedores com filtros")
    public ResponseEntity<ApiResponse<PageResponse<FornecedorDTOs.ResumoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("razaoSocial"));
        return ResponseEntity.ok(ApiResponse.success(
                fornecedorService.listar(principal.getTenantId(), nome, ativo, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualiza fornecedor")
    public ResponseEntity<ApiResponse<FornecedorDTOs.Response>> atualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody FornecedorDTOs.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fornecedor atualizado com sucesso",
                fornecedorService.atualizar(id, principal.getTenantId(), request)));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Ativa fornecedor")
    public ResponseEntity<ApiResponse<Void>> ativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        fornecedorService.ativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Fornecedor ativado"));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativa fornecedor")
    public ResponseEntity<ApiResponse<Void>> desativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        fornecedorService.desativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Fornecedor desativado"));
    }

    // =====================================================================
    // COMPRAS
    // =====================================================================

    @PostMapping("/compras")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lança uma nova compra (status inicial: PEDIDO)")
    public ResponseEntity<ApiResponse<FornecedorDTOs.CompraResponse>> criarCompra(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FornecedorDTOs.CompraCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Compra criada com sucesso",
                        fornecedorService.criarCompra(
                                principal.getTenantId(), principal.getId(), request)));
    }

    @PatchMapping("/compras/{id}/receber")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Marca compra como recebida e dá entrada no estoque")
    public ResponseEntity<ApiResponse<FornecedorDTOs.CompraResponse>> receberCompra(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Compra recebida. Estoque atualizado",
                fornecedorService.receberCompra(id, principal.getTenantId(), principal.getId())));
    }

    @PatchMapping("/compras/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cancela uma compra em estado de PEDIDO")
    public ResponseEntity<ApiResponse<FornecedorDTOs.CompraResponse>> cancelarCompra(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Compra cancelada",
                fornecedorService.cancelarCompra(id, principal.getTenantId())));
    }

    @GetMapping("/compras/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'FINANCEIRO')")
    @Operation(summary = "Busca compra por ID com itens")
    public ResponseEntity<ApiResponse<FornecedorDTOs.CompraResponse>> buscarCompraPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                fornecedorService.buscarCompraPorId(id, principal.getTenantId())));
    }

    @GetMapping("/compras")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'FINANCEIRO')")
    @Operation(summary = "Lista compras com filtros")
    public ResponseEntity<ApiResponse<PageResponse<FornecedorDTOs.CompraResumoResponse>>> listarCompras(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID fornecedorId,
            @RequestParam(required = false) StatusCompra status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dtCompra"));
        return ResponseEntity.ok(ApiResponse.success(
                fornecedorService.listarCompras(
                        principal.getTenantId(), fornecedorId, status, inicio, fim, pageable)));
    }
}
