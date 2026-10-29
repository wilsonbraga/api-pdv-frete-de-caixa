package com.pdv.modules.estoque.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdv.modules.estoque.dto.EstoqueDTOs;
import com.pdv.modules.estoque.entity.MovimentacaoEstoque;
import com.pdv.modules.estoque.entity.TipoMovimentacaoEstoque;
import com.pdv.modules.estoque.repository.MovimentacaoEstoqueRepository;
import com.pdv.modules.estoque.repository.MovimentacaoEstoqueSpecification;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.entity.ProdutoComponente;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Registra ajuste manual de estoque (entrada, saída ou ajuste por inventário).
     */
    @Transactional
    public EstoqueDTOs.MovimentacaoResponse ajustar(UUID tenantId, UUID usuarioId,
                                                     EstoqueDTOs.AjusteRequest request) {
        Produto produto = produtoRepository.findByIdAndTenantId(request.getProdutoId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.getProdutoId()));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (request.getTipo() == TipoMovimentacaoEstoque.SAIDA) {
            if (produto.estoqueInsuficiente(request.getQuantidade())) {
                throw new BusinessException(
                        String.format("Estoque insuficiente. Disponível: %s | Solicitado: %s",
                                produto.getEstoqueAtual(), request.getQuantidade()));
            }
        }

        BigDecimal anterior = produto.getEstoqueAtual();
        BigDecimal posterior = calcularPosterior(anterior, request.getQuantidade(), request.getTipo());

        produto.setEstoqueAtual(posterior);
        produtoRepository.save(produto);

        Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenantId).orElse(null);

        MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                .tenant(tenant)
                .produto(produto)
                .tipo(request.getTipo())
                .quantidade(request.getQuantidade())
                .quantidadeAnterior(anterior)
                .quantidadePosterior(posterior)
                .motivo(request.getMotivo())
                .usuario(usuario)
                .build();

        MovimentacaoEstoque salva = movimentacaoRepository.save(mov);
        log.info("Ajuste de estoque — produto: {} | tipo: {} | qtd: {} | anterior: {} → posterior: {}",
                produto.getId(), request.getTipo(), request.getQuantidade(), anterior, posterior);

        return toResponse(salva);
    }

    /**
     * Baixa de estoque automática ao finalizar venda. Chamado internamente pelo VendaService.
     */
    @Transactional
    public void baixarEstoqueVenda(UUID tenantId, UUID usuarioId, UUID vendaId,
                                   UUID produtoId, BigDecimal quantidade) {
        Produto produto = produtoRepository.findByIdAndTenantId(produtoId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (produto.estoqueInsuficiente(quantidade)) {
            throw new BusinessException(
                    String.format("Estoque insuficiente para '%s'. Disponível: %s | Solicitado: %s",
                            produto.getNome(), produto.getEstoqueAtual(), quantidade));
        }

        // Se produto composto, baixa cada componente
        if (produto.isComposto() && !produto.getComponentes().isEmpty()) {
            for (ProdutoComponente comp : produto.getComponentes()) {
                BigDecimal qtdComponente = comp.getQuantidade().multiply(quantidade);
                baixarEstoqueInterno(tenant, comp.getProdutoFilho(), qtdComponente,
                        TipoMovimentacaoEstoque.SAIDA, "Venda #" + vendaId, vendaId, "VENDA", usuarioId, tenantId);
            }
        } else {
            baixarEstoqueInterno(tenant, produto, quantidade,
                    TipoMovimentacaoEstoque.SAIDA, "Venda #" + vendaId, vendaId, "VENDA", usuarioId, tenantId);
        }
    }

    /**
     * Repõe estoque ao cancelar venda. Chamado internamente pelo VendaService.
     */
    @Transactional
    public void reporEstoqueCancelamento(UUID tenantId, UUID usuarioId, UUID vendaId,
                                          UUID produtoId, BigDecimal quantidade) {
        Produto produto = produtoRepository.findByIdAndTenantId(produtoId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (produto.isComposto() && !produto.getComponentes().isEmpty()) {
            for (ProdutoComponente comp : produto.getComponentes()) {
                BigDecimal qtdComponente = comp.getQuantidade().multiply(quantidade);
                entrarEstoqueInterno(tenant, comp.getProdutoFilho(), qtdComponente,
                        TipoMovimentacaoEstoque.DEVOLUCAO,
                        "Cancelamento venda #" + vendaId, vendaId, "CANCELAMENTO", usuarioId, tenantId);
            }
        } else {
            entrarEstoqueInterno(tenant, produto, quantidade,
                    TipoMovimentacaoEstoque.DEVOLUCAO,
                    "Cancelamento venda #" + vendaId, vendaId, "CANCELAMENTO", usuarioId, tenantId);
        }
    }

    /**
     * Entrada de estoque via compra. Chamado internamente pelo CompraService.
     */
    @Transactional
    public void entrarEstoqueCompra(UUID tenantId, UUID usuarioId, UUID compraId,
                                     UUID produtoId, BigDecimal quantidade) {
        Produto produto = produtoRepository.findByIdAndTenantId(produtoId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        entrarEstoqueInterno(tenant, produto, quantidade,
                TipoMovimentacaoEstoque.ENTRADA,
                "Compra #" + compraId, compraId, "COMPRA", usuarioId, tenantId);
    }

    /**
     * Processamento de inventário — ajusta o estoque de cada produto para a quantidade contada.
     */
    @Transactional
    public List<EstoqueDTOs.MovimentacaoResponse> processarInventario(
            UUID tenantId, UUID usuarioId,
            List<EstoqueDTOs.InventarioItemRequest> itens) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenantId).orElse(null);

        return itens.stream().map(item -> {
            Produto produto = produtoRepository.findByIdAndTenantId(item.getProdutoId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", item.getProdutoId()));

            BigDecimal anterior = produto.getEstoqueAtual();
            BigDecimal diferenca = item.getQuantidadeContada().subtract(anterior);

            produto.setEstoqueAtual(item.getQuantidadeContada());
            produtoRepository.save(produto);

            MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                    .tenant(tenant)
                    .produto(produto)
                    .tipo(TipoMovimentacaoEstoque.INVENTARIO)
                    .quantidade(diferenca.abs())
                    .quantidadeAnterior(anterior)
                    .quantidadePosterior(item.getQuantidadeContada())
                    .motivo("Ajuste de inventário periódico")
                    .usuario(usuario)
                    .build();

            return toResponse(movimentacaoRepository.save(mov));
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<EstoqueDTOs.MovimentacaoResponse> listarMovimentacoes(
            UUID tenantId, UUID produtoId, TipoMovimentacaoEstoque tipo,
            LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {

        // Usa Specification para evitar erro PostgreSQL "could not determine data type"
        // que ocorre com parâmetros null de tipos não-varchar em JPQL (:tipo IS NULL OR ...).
        var spec = MovimentacaoEstoqueSpecification.filtros(tenantId, produtoId, tipo, inicio, fim);
        Page<MovimentacaoEstoque> page = movimentacaoRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }
    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private void baixarEstoqueInterno(Tenant tenant, Produto produto, BigDecimal quantidade,
                                       TipoMovimentacaoEstoque tipo, String motivo,
                                       UUID referenciaId, String referenciaTipo,
                                       UUID usuarioId, UUID tenantId) {
        BigDecimal anterior = produto.getEstoqueAtual();
        BigDecimal posterior = anterior.subtract(quantidade);
        produto.setEstoqueAtual(posterior);
        produtoRepository.save(produto);

        Usuario usuario = usuarioId != null
                ? usuarioRepository.findByIdAndTenantId(usuarioId, tenantId).orElse(null)
                : null;

        movimentacaoRepository.save(MovimentacaoEstoque.builder()
                .tenant(tenant)
                .produto(produto)
                .tipo(tipo)
                .quantidade(quantidade)
                .quantidadeAnterior(anterior)
                .quantidadePosterior(posterior)
                .motivo(motivo)
                .referenciaId(referenciaId)
                .referenciaTipo(referenciaTipo)
                .usuario(usuario)
                .build());
    }

    private void entrarEstoqueInterno(Tenant tenant, Produto produto, BigDecimal quantidade,
                                       TipoMovimentacaoEstoque tipo, String motivo,
                                       UUID referenciaId, String referenciaTipo,
                                       UUID usuarioId, UUID tenantId) {
        BigDecimal anterior = produto.getEstoqueAtual();
        BigDecimal posterior = anterior.add(quantidade);
        produto.setEstoqueAtual(posterior);
        produtoRepository.save(produto);

        Usuario usuario = usuarioId != null
                ? usuarioRepository.findByIdAndTenantId(usuarioId, tenantId).orElse(null)
                : null;

        movimentacaoRepository.save(MovimentacaoEstoque.builder()
                .tenant(tenant)
                .produto(produto)
                .tipo(tipo)
                .quantidade(quantidade)
                .quantidadeAnterior(anterior)
                .quantidadePosterior(posterior)
                .motivo(motivo)
                .referenciaId(referenciaId)
                .referenciaTipo(referenciaTipo)
                .usuario(usuario)
                .build());
    }

    private BigDecimal calcularPosterior(BigDecimal anterior, BigDecimal quantidade,
                                          TipoMovimentacaoEstoque tipo) {
        return switch (tipo) {
            case ENTRADA, DEVOLUCAO -> anterior.add(quantidade);
            case SAIDA -> anterior.subtract(quantidade);
            case AJUSTE, INVENTARIO -> quantidade; // substitui o valor atual
        };
    }

    private EstoqueDTOs.MovimentacaoResponse toResponse(MovimentacaoEstoque mov) {
        EstoqueDTOs.MovimentacaoResponse r = new EstoqueDTOs.MovimentacaoResponse();
        r.setId(mov.getId());
        r.setProdutoId(mov.getProduto().getId());
        r.setNomeProduto(mov.getProduto().getNome());
        r.setTipo(mov.getTipo());
        r.setQuantidade(mov.getQuantidade());
        r.setQuantidadeAnterior(mov.getQuantidadeAnterior());
        r.setQuantidadePosterior(mov.getQuantidadePosterior());
        r.setMotivo(mov.getMotivo());
        r.setReferenciaId(mov.getReferenciaId());
        r.setReferenciaTipo(mov.getReferenciaTipo());
        r.setNomeUsuario(mov.getUsuario() != null ? mov.getUsuario().getNome() : null);
        r.setCriadoEm(mov.getCriadoEm());
        return r;
    }
}
