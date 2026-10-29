package com.pdv.modules.venda.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.venda.dto.VendaDTOs;
import com.pdv.modules.venda.entity.StatusVenda;
import com.pdv.modules.venda.service.VendaService;
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
@RequestMapping("/vendas")
@RequiredArgsConstructor
@Tag(name = "Vendas", description = "Frente de caixa — operações de venda")
public class VendaController {

    private final VendaService vendaService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Inicia uma nova venda (abre o carrinho)")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> iniciar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VendaDTOs.IniciarRequest request) {
        VendaDTOs.Response response = vendaService.iniciar(
                principal.getTenantId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venda iniciada", response));
    }

    @PostMapping("/{id}/itens")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adiciona ou incrementa um item na venda")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> adicionarItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody VendaDTOs.AdicionarItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item adicionado",
                vendaService.adicionarItem(id, principal.getTenantId(),
                        principal.getId(), request)));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela um item da venda")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> cancelarItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody VendaDTOs.CancelarItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item cancelado",
                vendaService.cancelarItem(id, itemId, principal.getTenantId(), request)));
    }

    @PatchMapping("/{id}/desconto")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aplica desconto global na venda (valor fixo ou percentual)")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> aplicarDesconto(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody VendaDTOs.AplicarDescontoRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Desconto aplicado",
                vendaService.aplicarDesconto(id, principal.getTenantId(),
                        principal.getId(), request)));
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Finaliza a venda com as formas de pagamento informadas")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> finalizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody VendaDTOs.FinalizarRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Venda finalizada com sucesso",
                vendaService.finalizar(id, principal.getTenantId(),
                        principal.getId(), request)));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cancela uma venda inteira — exige Gerente ou Admin")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> cancelar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody VendaDTOs.CancelarRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Venda cancelada com sucesso",
                vendaService.cancelar(id, principal.getTenantId(),
                        principal.getId(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca venda por ID com todos os detalhes")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                vendaService.buscarPorId(id, principal.getTenantId())));
    }

    @GetMapping("/numero/{numero}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca venda pelo número (ex: LOJA01-CX01-000123)")
    public ResponseEntity<ApiResponse<VendaDTOs.Response>> buscarPorNumero(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String numero) {
        return ResponseEntity.ok(ApiResponse.success(
                vendaService.buscarPorNumero(numero, principal.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista vendas com filtros e paginação")
    public ResponseEntity<ApiResponse<PageResponse<VendaDTOs.ResumoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID caixaId,
            @RequestParam(required = false) UUID pdvId,
            @RequestParam(required = false) UUID operadorId,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) StatusVenda status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "dtVenda"));
        return ResponseEntity.ok(ApiResponse.success(
                vendaService.listar(principal.getTenantId(), caixaId, pdvId, operadorId,
                        clienteId, status, inicio, fim, pageable)));
    }
}
