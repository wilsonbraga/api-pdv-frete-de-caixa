package com.pdv.modules.produto.controller;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.produto.dto.CategoriaDTOs;
import com.pdv.modules.produto.dto.ProdutoDTOs;
import com.pdv.modules.produto.service.ProdutoService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gestão de produtos e categorias")
public class ProdutoController {

    private final ProdutoService produtoService;

    // =====================================================================
    // PRODUTO
    // =====================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cria um novo produto")
    public ResponseEntity<ApiResponse<ProdutoDTOs.Response>> criar(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProdutoDTOs.CreateRequest request) {
        ProdutoDTOs.Response response = produtoService.criar(principal.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Produto criado com sucesso", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca produto por ID")
    public ResponseEntity<ApiResponse<ProdutoDTOs.Response>> buscarPorId(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(produtoService.buscarPorId(id, principal.getTenantId())));
    }

    @GetMapping("/codigo-barras/{codigoBarras}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca produto por código de barras — usado no PDV")
    public ResponseEntity<ApiResponse<ProdutoDTOs.Response>> buscarPorCodigoBarras(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String codigoBarras) {
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.buscarPorCodigoBarras(codigoBarras, principal.getTenantId())));
    }
    
    @GetMapping("/pdv/buscar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca produto por código de barras, SKU ou nome — usado no PDV")
    public ResponseEntity<ApiResponse<ProdutoDTOs.Response>> buscarParaPdv(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.buscarParaPdv(q, principal.getTenantId())));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista produtos com filtros e paginação")
    public ResponseEntity<ApiResponse<PageResponse<ProdutoDTOs.ResumoResponse>>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String codigoBarras,
            @RequestParam(required = false) Boolean estoqueAbaixoMinimo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nome") String sort) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.listar(principal.getTenantId(), nome, categoriaId,
                        ativo, codigoBarras, estoqueAbaixoMinimo, pageable)));
    }

    @GetMapping("/estoque-critico")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Lista produtos com estoque abaixo do mínimo")
    public ResponseEntity<ApiResponse<List<ProdutoDTOs.ResumoResponse>>> estoqueCritico(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.listarEstoqueCritico(principal.getTenantId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualiza produto")
    public ResponseEntity<ApiResponse<ProdutoDTOs.Response>> atualizar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ProdutoDTOs.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Produto atualizado com sucesso",
                produtoService.atualizar(id, principal.getTenantId(), request)));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Ativa produto")
    public ResponseEntity<ApiResponse<Void>> ativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        produtoService.ativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Produto ativado com sucesso"));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Desativa produto")
    public ResponseEntity<ApiResponse<Void>> desativar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        produtoService.desativar(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Produto desativado com sucesso"));
    }

    @GetMapping("/{id}/historico-preco")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Histórico de alterações de preço do produto")
    public ResponseEntity<ApiResponse<PageResponse<ProdutoDTOs.HistoricoPrecoResponse>>> historicoPreco(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.buscarHistoricoPreco(id, principal.getTenantId(), pageable)));
    }

    // =====================================================================
    // CATEGORIA
    // =====================================================================

    @PostMapping("/categorias")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Cria uma nova categoria de produto")
    public ResponseEntity<ApiResponse<CategoriaDTOs.Response>> criarCategoria(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategoriaDTOs.CreateRequest request) {
        CategoriaDTOs.Response response = produtoService.criarCategoria(
                principal.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Categoria criada com sucesso", response));
    }

    @GetMapping("/categorias")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista todas as categorias ativas (resumo flat)")
    public ResponseEntity<ApiResponse<List<CategoriaDTOs.ResumoResponse>>> listarCategorias(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.listarTodasCategorias(principal.getTenantId())));
    }

    @GetMapping("/categorias/arvore")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista categorias raiz com filhas aninhadas (árvore)")
    public ResponseEntity<ApiResponse<List<CategoriaDTOs.Response>>> arvore(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                produtoService.listarCategoriasRaiz(principal.getTenantId())));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualiza nome de uma categoria")
    public ResponseEntity<ApiResponse<CategoriaDTOs.Response>> atualizarCategoria(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CategoriaDTOs.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Categoria atualizada com sucesso",
                produtoService.atualizarCategoria(id, principal.getTenantId(), request)));
    }

    @DeleteMapping("/categorias/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa uma categoria")
    public ResponseEntity<ApiResponse<Void>> desativarCategoria(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        produtoService.desativarCategoria(id, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.success("Categoria desativada com sucesso"));
    }
}
