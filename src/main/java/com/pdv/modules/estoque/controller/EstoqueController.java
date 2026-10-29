package com.pdv.modules.estoque.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.estoque.dto.EstoqueDTOs;
import com.pdv.modules.estoque.entity.TipoMovimentacaoEstoque;
import com.pdv.modules.estoque.service.EstoqueService;
import com.pdv.shared.dto.ApiResponse;
import com.pdv.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Controle de movimentações de estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @PostMapping("/ajuste")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Realiza ajuste manual de estoque (entrada ou saída avulsa)")
    public ResponseEntity<ApiResponse<EstoqueDTOs.MovimentacaoResponse>> ajustar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EstoqueDTOs.AjusteRequest request) {
        EstoqueDTOs.MovimentacaoResponse response = estoqueService.ajustar(
                principal.getTenantId(), principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Ajuste realizado com sucesso", response));
    }

    @PostMapping("/inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Processa inventário periódico — ajusta estoque pelos valores contados")
    public ResponseEntity<ApiResponse<List<EstoqueDTOs.MovimentacaoResponse>>> processarInventario(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody List<EstoqueDTOs.InventarioItemRequest> itens) {
        List<EstoqueDTOs.MovimentacaoResponse> response = estoqueService.processarInventario(
                principal.getTenantId(), principal.getId(), itens);
        return ResponseEntity.ok(ApiResponse.success(
                "Inventário processado: " + response.size() + " produto(s) ajustado(s)", response));
    }

    @GetMapping("/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista movimentações de estoque com filtros")
    public ResponseEntity<ApiResponse<PageResponse<EstoqueDTOs.MovimentacaoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) TipoMovimentacaoEstoque tipo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "criadoEm"));
        return ResponseEntity.ok(ApiResponse.success(
                estoqueService.listarMovimentacoes(
                        principal.getTenantId(), produtoId, tipo, inicio, fim, pageable)));
    }
}
