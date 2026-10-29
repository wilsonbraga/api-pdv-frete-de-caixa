package com.pdv.modules.catalogo.controller;

import com.pdv.modules.catalogo.dto.CatalogoDTOs;
import com.pdv.modules.catalogo.service.CatalogoService;
import com.pdv.shared.dto.ApiResponse;
import com.pdv.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService service;

    /** Lista todas as categorias do catálogo global */
    @GetMapping("/categorias")
    public ResponseEntity<ApiResponse<List<CatalogoDTOs.CategoriaResponse>>> categorias() {
        return ResponseEntity.ok(ApiResponse.success(service.listarCategorias()));
    }

    /** Lista produtos do catálogo com filtros */
    @GetMapping("/produtos")
    public ResponseEntity<ApiResponse<PageResponse<CatalogoDTOs.ProdutoResponse>>> produtos(
            @RequestParam(required = false) String segmento,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "50")  int size) {

        var pageable = PageRequest.of(page, size, Sort.by("nome"));
        var result   = service.listarProdutos(segmento, categoriaId, nome, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    /**
     * Importa produtos do catálogo para o tenant autenticado.
     * Apenas ADMIN e GERENTE podem importar.
     */
    @PostMapping("/importar/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResponseEntity<ApiResponse<CatalogoDTOs.ImportarResponse>> importar(
            @PathVariable UUID tenantId,
            @RequestBody  CatalogoDTOs.ImportarRequest req) {

        return ResponseEntity.ok(
                ApiResponse.success(service.importar(tenantId, req))
        );
    }
}
