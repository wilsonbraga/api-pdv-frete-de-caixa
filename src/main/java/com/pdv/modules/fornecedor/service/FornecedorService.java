package com.pdv.modules.fornecedor.service;

import com.pdv.modules.estoque.service.EstoqueService;
import com.pdv.modules.fornecedor.dto.FornecedorDTOs;
import com.pdv.modules.fornecedor.dto.FornecedorMapper;
import com.pdv.modules.fornecedor.entity.*;
import com.pdv.modules.fornecedor.repository.CompraRepository;
import com.pdv.modules.fornecedor.repository.FornecedorRepository;
import com.pdv.modules.produto.entity.Produto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final CompraRepository compraRepository;
    private final ProdutoRepository produtoRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;
    private final FornecedorMapper fornecedorMapper;

    // =====================================================================
    // FORNECEDOR
    // =====================================================================

    @Transactional
    public FornecedorDTOs.Response criar(UUID tenantId, FornecedorDTOs.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (request.getCnpj() != null && !request.getCnpj().isBlank()) {
            if (fornecedorRepository.existsByCnpjAndTenantId(request.getCnpj(), tenantId)) {
                throw new BusinessException(
                        "Já existe fornecedor com CNPJ: " + request.getCnpj());
            }
        }

        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        fornecedor.setTenant(tenant);
        Fornecedor salvo = fornecedorRepository.save(fornecedor);
        log.info("Fornecedor criado: {} ({}) no tenant: {}",
                salvo.getRazaoSocial(), salvo.getId(), tenantId);
        return fornecedorMapper.toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public FornecedorDTOs.Response buscarPorId(UUID id, UUID tenantId) {
        return fornecedorMapper.toResponse(
                fornecedorRepository.findByIdAndTenantId(id, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<FornecedorDTOs.ResumoResponse> listar(UUID tenantId, String nome,
                                                                Boolean ativo, Pageable pageable) {
        return PageResponse.of(
                fornecedorRepository.findByFiltros(tenantId, nome, ativo, pageable)
                        .map(fornecedorMapper::toResumoResponse));
    }

    @Transactional
    public FornecedorDTOs.Response atualizar(UUID id, UUID tenantId,
                                              FornecedorDTOs.UpdateRequest request) {
        Fornecedor fornecedor = fornecedorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
        fornecedorMapper.updateFromRequest(request, fornecedor);
        return fornecedorMapper.toResponse(fornecedorRepository.save(fornecedor));
    }

    @Transactional
    public void ativar(UUID id, UUID tenantId) {
        Fornecedor f = fornecedorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
        f.setAtivo(true);
        fornecedorRepository.save(f);
    }

    @Transactional
    public void desativar(UUID id, UUID tenantId) {
        Fornecedor f = fornecedorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
        f.setAtivo(false);
        fornecedorRepository.save(f);
    }

    // =====================================================================
    // COMPRAS
    // =====================================================================

    @Transactional
    public FornecedorDTOs.CompraResponse criarCompra(UUID tenantId, UUID usuarioId,
                                                       FornecedorDTOs.CompraCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Fornecedor fornecedor = fornecedorRepository
                .findByIdAndTenantId(request.getFornecedorId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor",
                        request.getFornecedorId()));

        if (!fornecedor.isAtivo()) {
            throw new BusinessException("Fornecedor inativo. Não é possível realizar compra");
        }

        Usuario usuario = usuarioRepository.findByIdAndTenantId(usuarioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        Compra compra = Compra.builder()
                .tenant(tenant)
                .fornecedor(fornecedor)
                .numeroNota(request.getNumeroNota())
                .dtCompra(LocalDateTime.now())
                .dtEntrega(request.getDtEntrega())
                .status(StatusCompra.PEDIDO)
                .observacao(request.getObservacao())
                .usuario(usuario)
                .total(java.math.BigDecimal.ZERO)
                .build();

        List<ItemCompra> itens = new ArrayList<>();
        for (FornecedorDTOs.ItemCompraRequest ir : request.getItens()) {
            Produto produto = produtoRepository.findByIdAndTenantId(ir.getProdutoId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", ir.getProdutoId()));

            ItemCompra item = ItemCompra.builder()
                    .compra(compra)
                    .produto(produto)
                    .quantidade(ir.getQuantidade())
                    .precoCusto(ir.getPrecoCusto())
                    .totalItem(ir.getPrecoCusto().multiply(ir.getQuantidade()))
                    .build();

            itens.add(item);
        }

        compra.getItens().addAll(itens);
        compra.setTotal(compra.calcularTotal());

        Compra salva = compraRepository.save(compra);
        log.info("Compra criada: {} | Fornecedor: {} | Total: R$ {}",
                salva.getId(), fornecedor.getRazaoSocial(), salva.getTotal());

        return fornecedorMapper.toCompraResponse(salva);
    }

    @Transactional
    public FornecedorDTOs.CompraResponse receberCompra(UUID compraId, UUID tenantId,
                                                         UUID usuarioId) {
        Compra compra = compraRepository.findByIdComDetalhes(compraId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", compraId));

        if (compra.getStatus() != StatusCompra.PEDIDO) {
            throw new BusinessException(
                    "Compra não pode ser recebida. Status atual: " + compra.getStatus());
        }

        compra.setStatus(StatusCompra.RECEBIDO);
        compra.setDtEntrega(LocalDateTime.now());

        // Dá entrada no estoque de cada item
        for (ItemCompra item : compra.getItens()) {
            estoqueService.entrarEstoqueCompra(
                    tenantId, usuarioId, compraId,
                    item.getProduto().getId(), item.getQuantidade());

            // Atualiza preço de custo do produto se configurado
            Produto produto = item.getProduto();
            produto.setPrecoCusto(item.getPrecoCusto());
            produtoRepository.save(produto);
        }

        Compra salva = compraRepository.save(compra);
        log.info("Compra recebida: {} | Fornecedor: {} | {} itens processados",
                compraId, compra.getFornecedor().getRazaoSocial(), compra.getItens().size());

        return fornecedorMapper.toCompraResponse(salva);
    }

    @Transactional
    public FornecedorDTOs.CompraResponse cancelarCompra(UUID compraId, UUID tenantId) {
        Compra compra = compraRepository.findByIdComDetalhes(compraId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra", compraId));

        if (compra.getStatus() == StatusCompra.RECEBIDO) {
            throw new BusinessException(
                    "Não é possível cancelar uma compra já recebida no estoque");
        }
        if (compra.getStatus() == StatusCompra.CANCELADO) {
            throw new BusinessException("Compra já está cancelada");
        }

        compra.setStatus(StatusCompra.CANCELADO);
        Compra salva = compraRepository.save(compra);
        log.info("Compra cancelada: {}", compraId);
        return fornecedorMapper.toCompraResponse(salva);
    }

    @Transactional(readOnly = true)
    public FornecedorDTOs.CompraResponse buscarCompraPorId(UUID compraId, UUID tenantId) {
        return fornecedorMapper.toCompraResponse(
                compraRepository.findByIdComDetalhes(compraId, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Compra", compraId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<FornecedorDTOs.CompraResumoResponse> listarCompras(
            UUID tenantId, UUID fornecedorId, StatusCompra status,
            LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        return PageResponse.of(
                compraRepository.findByFiltros(tenantId, fornecedorId, status, inicio, fim, pageable)
                        .map(fornecedorMapper::toCompraResumoResponse));
    }
}
