package com.pdv.modules.produto.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdv.modules.auth.security.UserPrincipal;
import com.pdv.modules.produto.dto.CategoriaDTOs;
import com.pdv.modules.produto.dto.ProdutoDTOs;
import com.pdv.modules.produto.dto.ProdutoMapper;
import com.pdv.modules.produto.entity.CategoriaProduto;
import com.pdv.modules.produto.entity.HistoricoPreco;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.fornecedor.entity.Fornecedor;
import com.pdv.modules.fornecedor.repository.FornecedorRepository;
import com.pdv.modules.produto.entity.ProdutoComponente;
import com.pdv.modules.produto.repository.CategoriaProdutoRepository;
import com.pdv.modules.produto.repository.HistoricoPrecoRepository;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaProdutoRepository categoriaRepository;
    private final HistoricoPrecoRepository historicoPrecoRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoMapper produtoMapper;
    private final FornecedorRepository fornecedorRepository;

    // =====================================================================
    // PRODUTO
    // =====================================================================

    @Transactional
    public ProdutoDTOs.Response criar(UUID tenantId, ProdutoDTOs.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        validarCodigoBarrasUnico(request.getCodigoBarras(), tenantId, null);
        validarSkuUnico(request.getSku(), tenantId, null);

        Produto produto = produtoMapper.toEntity(request);
        produto.setTenant(tenant);

        if (request.getCategoriaId() != null) {
            CategoriaProduto categoria = categoriaRepository
                    .findByIdAndTenantId(request.getCategoriaId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria",
                            request.getCategoriaId()));
            produto.setCategoria(categoria);
        }

        // Resolver fornecedor principal (opcional)
        if (request.getFornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository
                    .findByIdAndTenantId(request.getFornecedorId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor",
                            request.getFornecedorId()));
            produto.setFornecedor(fornecedor);
        }

        // Resolver fornecedor principal (opcional)
        if (request.getFornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository
                    .findByIdAndTenantId(request.getFornecedorId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor",
                            request.getFornecedorId()));
            produto.setFornecedor(fornecedor);
        }

        if (request.isComposto() && request.getComponentes() != null) {
            produto.getComponentes().addAll(montarComponentes(produto, request.getComponentes(), tenantId));
        }

        Produto salvo = produtoRepository.save(produto);

        registrarHistoricoPreco(salvo, tenantId);

        log.info("Produto criado: {} ({}) no tenant: {}", salvo.getNome(), salvo.getId(), tenantId);
        return toResponseCompleto(salvo);
    }

    @Transactional(readOnly = true)
    public ProdutoDTOs.Response buscarPorId(UUID id, UUID tenantId) {
        Produto produto = produtoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        return toResponseCompleto(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoDTOs.Response buscarPorCodigoBarras(String codigoBarras, UUID tenantId) {
        Produto produto = produtoRepository.findByCodigoBarrasAndTenantId(codigoBarras, tenantId)
                .orElseThrow(() -> new BusinessException(
                        "Produto não encontrado com código de barras: " + codigoBarras));
        return toResponseCompleto(produto);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProdutoDTOs.ResumoResponse> listar(UUID tenantId, String nome,
                                                            UUID categoriaId, Boolean ativo,
                                                            String codigoBarras,
                                                            Boolean estoqueAbaixoMinimo,
                                                            Pageable pageable) {
        Page<Produto> page = produtoRepository.findByFiltros(
                tenantId, nome, categoriaId, ativo, codigoBarras, estoqueAbaixoMinimo, pageable);
        return PageResponse.of(page.map(produtoMapper::toResumoResponse));
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTOs.ResumoResponse> listarEstoqueCritico(UUID tenantId) {
        return produtoRepository.findEstoqueCritico(tenantId)
                .stream()
                .map(produtoMapper::toResumoResponse)
                .collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public ProdutoDTOs.Response buscarParaPdv(String query, UUID tenantId) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("Informe um código de barras, SKU ou nome para buscar");
        }
        String q = query.trim();

        // 1. Tenta por código de barras (busca exata)
        Optional<Produto> porBarras = produtoRepository.findByCodigoBarrasAndTenantId(q, tenantId);
        if (porBarras.isPresent()) {
            return toResponseCompleto(porBarras.get());
        }

        // 2. Tenta por SKU (busca exata)
        Optional<Produto> porSku = produtoRepository.findBySkuAndTenantId(q, tenantId);
        if (porSku.isPresent()) {
            return toResponseCompleto(porSku.get());
        }

        // 3. Busca por nome contendo o termo (LIKE) — prioriza correspondência exata
        List<Produto> porNome = produtoRepository.findByNomeContendoAndTenantId(
                q, tenantId, PageRequest.of(0, 1));  // pega só o mais relevante

        if (!porNome.isEmpty()) {
            return toResponseCompleto(porNome.get(0));
        }

        throw new ResourceNotFoundException("Produto não encontrado: " + q);
    }

    @Transactional
    public ProdutoDTOs.Response atualizar(UUID id, UUID tenantId, ProdutoDTOs.UpdateRequest request) {
        Produto produto = produtoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        validarCodigoBarrasUnico(request.getCodigoBarras(), tenantId, id);
        validarSkuUnico(request.getSku(), tenantId, id);

        BigDecimal precoCustoAnterior = produto.getPrecoCusto();
        BigDecimal precoVendaAnterior = produto.getPrecoVenda();

        produtoMapper.updateFromRequest(request, produto);

        if (request.getCategoriaId() != null) {
            CategoriaProduto categoria = categoriaRepository
                    .findByIdAndTenantId(request.getCategoriaId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria",
                            request.getCategoriaId()));
            produto.setCategoria(categoria);
        } else {
            produto.setCategoria(null);
        }

        // Resolver fornecedor principal (opcional)
        if (request.getFornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository
                    .findByIdAndTenantId(request.getFornecedorId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor",
                            request.getFornecedorId()));
            produto.setFornecedor(fornecedor);
        } else {
            produto.setFornecedor(null);  // permite remover vínculo
        }

        // Resolver fornecedor principal (opcional — null remove o vínculo)
        if (request.getFornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository
                    .findByIdAndTenantId(request.getFornecedorId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor",
                            request.getFornecedorId()));
            produto.setFornecedor(fornecedor);
        } else {
            produto.setFornecedor(null);
        }

        // Atualiza componentes se produto composto
        if (produto.isComposto() && request.getComponentes() != null) {
            produto.getComponentes().clear();
            produto.getComponentes().addAll(
                    montarComponentes(produto, request.getComponentes(), tenantId));
        }

        Produto salvo = produtoRepository.save(produto);

        // Registra histórico apenas se o preço mudou
        boolean precoMudou = precoCustoAnterior.compareTo(salvo.getPrecoCusto()) != 0
                || precoVendaAnterior.compareTo(salvo.getPrecoVenda()) != 0;
        if (precoMudou) {
            registrarHistoricoPreco(salvo, tenantId);
            log.info("Preço atualizado para produto: {} — custo: {} → {} | venda: {} → {}",
                    id, precoCustoAnterior, salvo.getPrecoCusto(),
                    precoVendaAnterior, salvo.getPrecoVenda());
        }

        log.info("Produto atualizado: {}", id);
        return toResponseCompleto(salvo);
    }

    @Transactional
    public void ativar(UUID id, UUID tenantId) {
        Produto produto = produtoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        produto.setAtivo(true);
        produtoRepository.save(produto);
    }

    @Transactional
    public void desativar(UUID id, UUID tenantId) {
        Produto produto = produtoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    @Transactional
    public void atualizarFoto(UUID id, UUID tenantId, String fotoUrl) {
        Produto produto = produtoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        produto.setFotoUrl(fotoUrl);
        produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProdutoDTOs.HistoricoPrecoResponse> buscarHistoricoPreco(
            UUID id, UUID tenantId, Pageable pageable) {
        produtoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        var page = historicoPrecoRepository.findByProdutoIdOrderByCriadoEmDesc(id, pageable);
        return PageResponse.of(page.map(produtoMapper::toHistoricoResponse));
    }

    // =====================================================================
    // CATEGORIA
    // =====================================================================

    @Transactional
    public CategoriaDTOs.Response criarCategoria(UUID tenantId, CategoriaDTOs.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        CategoriaProduto categoria = produtoMapper.toCategoriaEntity(request);
        categoria.setTenant(tenant);

        if (request.getPaiId() != null) {
            CategoriaProduto pai = categoriaRepository
                    .findByIdAndTenantId(request.getPaiId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria pai",
                            request.getPaiId()));
            categoria.setPai(pai);
        }

        CategoriaProduto salva = categoriaRepository.save(categoria);
        log.info("Categoria criada: {} ({}) no tenant: {}", salva.getNome(), salva.getId(), tenantId);
        return toCategoriaResponseComFilhas(salva);
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTOs.Response> listarCategoriasRaiz(UUID tenantId) {
        return categoriaRepository.findRaizesByTenantId(tenantId)
                .stream()
                .map(this::toCategoriaResponseComFilhas)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTOs.ResumoResponse> listarTodasCategorias(UUID tenantId) {
        return categoriaRepository.findByTenantIdAndAtivoTrueOrderByNome(tenantId)
                .stream()
                .map(produtoMapper::toCategoriaResumoResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaDTOs.Response atualizarCategoria(UUID id, UUID tenantId,
                                                      CategoriaDTOs.UpdateRequest request) {
        CategoriaProduto categoria = categoriaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));
        categoria.setNome(request.getNome());
        CategoriaProduto salva = categoriaRepository.save(categoria);
        return toCategoriaResponseComFilhas(salva);
    }

    @Transactional
    public void desativarCategoria(UUID id, UUID tenantId) {
        CategoriaProduto categoria = categoriaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));
        categoria.setAtivo(false);
        categoriaRepository.save(categoria);
    }

    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private void validarCodigoBarrasUnico(String codigoBarras, UUID tenantId, UUID idExcluir) {
        if (codigoBarras == null || codigoBarras.isBlank()) return;
        boolean existe = produtoRepository.existsByCodigoBarrasAndTenantId(codigoBarras, tenantId);
        if (existe) {
            if (idExcluir != null) {
                produtoRepository.findByCodigoBarrasAndTenantId(codigoBarras, tenantId)
                        .ifPresent(p -> {
                            if (!p.getId().equals(idExcluir)) {
                                throw new BusinessException(
                                        "Já existe produto com o código de barras: " + codigoBarras);
                            }
                        });
            } else {
                throw new BusinessException(
                        "Já existe produto com o código de barras: " + codigoBarras);
            }
        }
    }

    private void validarSkuUnico(String sku, UUID tenantId, UUID idExcluir) {
        if (sku == null || sku.isBlank()) return;
        if (produtoRepository.existsBySkuAndTenantId(sku, tenantId)) {
            throw new BusinessException("Já existe produto com o SKU: " + sku);
        }
    }

    private List<ProdutoComponente> montarComponentes(Produto produtoPai,
                                                       List<ProdutoDTOs.ComponenteRequest> requests,
                                                       UUID tenantId) {
        List<ProdutoComponente> componentes = new ArrayList<>();
        for (ProdutoDTOs.ComponenteRequest cr : requests) {
            Produto filho = produtoRepository.findByIdAndTenantId(cr.getProdutoFilhoId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto componente",
                            cr.getProdutoFilhoId()));
            if (filho.isComposto()) {
                throw new BusinessException(
                        "Produto composto não pode ser componente de outro produto composto");
            }
            ProdutoComponente componente = ProdutoComponente.builder()
                    .produtoPai(produtoPai)
                    .produtoFilho(filho)
                    .quantidade(cr.getQuantidade())
                    .build();
            componentes.add(componente);
        }
        return componentes;
    }

    private void registrarHistoricoPreco(Produto produto, UUID tenantId) {
        UUID usuarioId = getUsuarioIdContexto();
        HistoricoPreco historico = HistoricoPreco.builder()
                .produto(produto)
                .precoCusto(produto.getPrecoCusto())
                .precoVenda(produto.getPrecoVenda())
                .usuario(usuarioId != null
                        ? usuarioRepository.findByIdAndTenantId(usuarioId, tenantId).orElse(null)
                        : null)
                .build();
        historicoPrecoRepository.save(historico);
    }

    private UUID getUsuarioIdContexto() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
                return p.getId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ProdutoDTOs.Response toResponseCompleto(Produto produto) {
        ProdutoDTOs.Response response = produtoMapper.toResponse(produto);
        if (produto.getComponentes() != null) {
            response.setComponentes(produto.getComponentes()
                    .stream()
                    .map(produtoMapper::toComponenteResponse)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    private CategoriaDTOs.Response toCategoriaResponseComFilhas(CategoriaProduto categoria) {
        CategoriaDTOs.Response response = produtoMapper.toCategoriaResponse(categoria);
        List<CategoriaDTOs.Response> filhas = categoriaRepository
                .findFilhasByPaiId(categoria.getTenant().getId(), categoria.getId())
                .stream()
                .map(this::toCategoriaResponseComFilhas)
                .collect(Collectors.toList());
        response.setFilhas(filhas);
        return response;
    }

    
}
