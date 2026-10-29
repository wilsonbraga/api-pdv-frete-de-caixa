package com.pdv.modules.cliente.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.cliente.dto.ClienteDTOs;
import com.pdv.modules.cliente.service.ClienteService;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro e gestão de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cria um novo cliente")
    public ResponseEntity<ApiResponse<ClienteDTOs.Response>> criar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClienteDTOs.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cliente criado com sucesso",
                        clienteService.criar(principal.getTenantId(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca cliente por ID")
    public ResponseEntity<ApiResponse<ClienteDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                clienteService.buscarPorId(id, principal.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista clientes com filtros")
    public ResponseEntity<ApiResponse<PageResponse<ClienteDTOs.ResumoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpfCnpj,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean comSaldoDevedor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nome"));
        return ResponseEntity.ok(ApiResponse.success(
                clienteService.listar(principal.getTenantId(), nome, cpfCnpj, ativo,
                        comSaldoDevedor, pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualiza dados do cliente")
    public ResponseEntity<ApiResponse<ClienteDTOs.Response>> atualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ClienteDTOs.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cliente atualizado com sucesso",
                clienteService.atualizar(id, principal.getTenantId(), request)));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Ativa cliente")
    public ResponseEntity<ApiResponse<Void>> ativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        clienteService.ativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Cliente ativado"));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativa cliente")
    public ResponseEntity<ApiResponse<Void>> desativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        clienteService.desativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Cliente desativado"));
    }

    @PostMapping("/{id}/receber-fiado")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'FINANCEIRO')")
    @Operation(summary = "Registra recebimento de fiado (reduz saldo devedor)")
    public ResponseEntity<ApiResponse<ClienteDTOs.Response>> receberFiado(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ClienteDTOs.AtualizarSaldoRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fiado recebido com sucesso",
                clienteService.receberFiado(id, principal.getTenantId(), request)));
    }
}
