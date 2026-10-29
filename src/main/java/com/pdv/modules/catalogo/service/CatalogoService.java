package com.pdv.modules.catalogo.service;

import com.pdv.modules.catalogo.dto.CatalogoDTOs;
import com.pdv.modules.catalogo.entity.ProdutoCatalogo;
import com.pdv.modules.catalogo.repository.CatalogoCategoriaRepository;
import com.pdv.modules.catalogo.repository.ProdutoCatalogoRepository;
import com.pdv.modules.produto.entity.CategoriaProduto;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.repository.CategoriaProdutoRepository;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProdutoCatalogoRepository   catalogoRepo;
    private final CatalogoCategoriaRepository catRepo;
    private final ProdutoRepository           produtoRepo;
    private final CategoriaProdutoRepository  categoriaProdutoRepo;
    private final TenantRepository            tenantRepo;

    // ── Listagem ──────────────────────────────────────────────────────────

    public List<CatalogoDTOs.CategoriaResponse> listarCategorias() {
        return catRepo.findAllByOrderByOrdemAsc().stream().map(c -> {
            var dto = new CatalogoDTOs.CategoriaResponse();
            dto.setId(c.getId());
            dto.setNome(c.getNome());
            dto.setSegmento(c.getSegmento());
            dto.setIcone(c.getIcone());
            dto.setOrdem(c.getOrdem());
            return dto;
        }).toList();
    }

    public Page<CatalogoDTOs.ProdutoResponse> listarProdutos(
            String segmento, UUID categoriaId, String nome, Pageable pageable) {

        // Usa Specification para evitar erro PostgreSQL "lower(bytea) does not exist"
        // que ocorre com JPQL quando parâmetros nullable são passados ao LOWER()
        var spec = ProdutoCatalogoRepository.comFiltros(segmento, categoriaId, nome);
        return catalogoRepo.findAll(spec, pageable).map(this::toDto);
    }

    // ── Importação ────────────────────────────────────────────────────────

    @Transactional
    public CatalogoDTOs.ImportarResponse importar(UUID tenantId, CatalogoDTOs.ImportarRequest req) {

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant não encontrado"));

        List<ProdutoCatalogo> produtos = buscarProdutosParaImportar(req);

        int importados = 0, ignorados = 0, erros = 0;
        List<String> detalhes = new ArrayList<>();
        Map<String, CategoriaProduto> catCache = new HashMap<>();

        for (ProdutoCatalogo pc : produtos) {
            try {
                // Checar duplicata por código de barras
                if (pc.getCodigoBarras() != null && !pc.getCodigoBarras().isBlank()) {
                    boolean existe = produtoRepo
                            .existsByCodigoBarrasAndTenantId(pc.getCodigoBarras(), tenantId);
                    if (existe) {
                        if (!req.isSobrescrever()) {
                            ignorados++;
                            continue;
                        }
                        produtoRepo.findByCodigoBarrasAndTenantId(pc.getCodigoBarras(), tenantId)
                                .ifPresent(p -> {
                                    p.setPrecoVenda(pc.getPrecoSugerido());
                                    produtoRepo.save(p);
                                });
                        importados++;
                        continue;
                    }
                }

                // Resolver/criar categoria do tenant pelo nome
                String cacheKey = pc.getCategoria().getNome();
                CategoriaProduto cat = catCache.computeIfAbsent(cacheKey, nomeCat -> {
                    var lista = categoriaProdutoRepo.findByTenantIdAndAtivoTrueOrderByNome(tenantId);
                    return lista.stream()
                            .filter(c -> c.getNome().equalsIgnoreCase(nomeCat))
                            .findFirst()
                            .orElseGet(() -> categoriaProdutoRepo.save(
                                    CategoriaProduto.builder()
                                            .tenant(tenant)
                                            .nome(nomeCat)
                                            .ativo(true)
                                            .build()
                            ));
                });

                Produto novo = Produto.builder()
                        .tenant(tenant)
                        .categoria(cat)
                        .nome(pc.getNome())
                        .codigoBarras(pc.getCodigoBarras())
                        .ncm(pc.getNcm())
                        .unidade(pc.getUnidade())
                        .precoCusto(BigDecimal.ZERO)
                        .precoVenda(pc.getPrecoSugerido())
                        .estoqueAtual(BigDecimal.ZERO)
                        .estoqueMinimo(BigDecimal.ZERO)
                        .ativo(true)
                        .composto(false)
                        .build();

                produtoRepo.save(novo);
                importados++;

            } catch (Exception ex) {
                log.warn("Erro ao importar '{}': {}", pc.getNome(), ex.getMessage());
                erros++;
                detalhes.add("Erro em '" + pc.getNome() + "': " + ex.getMessage());
            }
        }

        var resp = new CatalogoDTOs.ImportarResponse();
        resp.setImportados(importados);
        resp.setIgnorados(ignorados);
        resp.setErros(erros);
        resp.setDetalhes(detalhes);
        return resp;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<ProdutoCatalogo> buscarProdutosParaImportar(CatalogoDTOs.ImportarRequest req) {
        if (req.getCategoriaIds() != null && !req.getCategoriaIds().isEmpty())
            return catalogoRepo.findByCategoriaIds(req.getCategoriaIds());
        if (req.getSegmentos() != null && !req.getSegmentos().isEmpty())
            return catalogoRepo.findBySegmentos(req.getSegmentos());
        return catalogoRepo.findAll();
    }

    private CatalogoDTOs.ProdutoResponse toDto(ProdutoCatalogo p) {
        var dto = new CatalogoDTOs.ProdutoResponse();
        dto.setId(p.getId());
        dto.setNome(p.getNome());
        dto.setCodigoBarras(p.getCodigoBarras());
        dto.setNcm(p.getNcm());
        dto.setUnidade(p.getUnidade());
        dto.setPrecoSugerido(p.getPrecoSugerido());
        dto.setDescricao(p.getDescricao());
        if (p.getCategoria() != null) {
            dto.setCategoriaId(p.getCategoria().getId());
            dto.setCategoriaNome(p.getCategoria().getNome());
            dto.setCategoriaSegmento(p.getCategoria().getSegmento());
        }
        return dto;
    }
}
