package com.pdv.modules.venda.service;

import com.pdv.modules.caixa.entity.Caixa;
import com.pdv.modules.caixa.entity.StatusCaixa;
import com.pdv.modules.caixa.repository.CaixaRepository;
import com.pdv.modules.caixa.service.CaixaService;
import com.pdv.modules.cliente.entity.Cliente;
import com.pdv.modules.cliente.repository.ClienteRepository;
import com.pdv.modules.cliente.service.ClienteService;
import com.pdv.modules.estoque.service.EstoqueService;
import com.pdv.modules.produto.entity.Produto;
import com.pdv.modules.produto.repository.ProdutoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.modules.venda.dto.VendaDTOs;
import com.pdv.modules.venda.entity.*;
import com.pdv.modules.venda.repository.VendaRepository;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendaService {

    private final VendaRepository vendaRepository;
    private final CaixaRepository caixaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final EstoqueService estoqueService;
    private final CaixaService caixaService;
    private final ClienteService clienteService;
    private final NumeracaoVendaService numeracaoVendaService;

    // =====================================================================
    // INICIAR VENDA
    // =====================================================================

    @Transactional
    public VendaDTOs.Response iniciar(UUID tenantId, UUID operadorId,
                                       VendaDTOs.IniciarRequest request) {
        Caixa caixa = caixaRepository.findByIdAndTenantId(request.getCaixaId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", request.getCaixaId()));

        if (!caixa.isAberto()) {
            throw new BusinessException("Caixa está fechado. Abra o caixa para realizar vendas");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findByIdAndTenantId(request.getClienteId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente",
                            request.getClienteId()));
            if (!cliente.isAtivo()) {
                throw new BusinessException("Cliente inativo. Selecione um cliente ativo");
            }
        }

        String numeroVenda = numeracaoVendaService.gerarNumero(tenantId, caixa.getPdv().getNome());

        Venda venda = Venda.builder()
                .tenant(tenant)
                .caixa(caixa)
                .pdv(caixa.getPdv())
                .operador(operador)
                .cliente(cliente)
                .numeroVenda(numeroVenda)
                .dtVenda(LocalDateTime.now())
                .subtotal(BigDecimal.ZERO)
                .desconto(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .status(StatusVenda.ABERTA)
                .sincronizado(false)
                .deviceId(request.getDeviceId())
                .build();

        Venda salva = vendaRepository.save(venda);
        log.info("Venda iniciada: {} | Operador: {} | Caixa: {}",
                salva.getNumeroVenda(), operador.getNome(), caixa.getId());

        return toResponse(salva);
    }

    // =====================================================================
    // ADICIONAR ITEM
    // =====================================================================

    @Transactional
    public VendaDTOs.Response adicionarItem(UUID vendaId, UUID tenantId, UUID operadorId,
                                             VendaDTOs.AdicionarItemRequest request) {
        Venda venda = vendaRepository.findByIdComItens(vendaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", vendaId));

        validarVendaAberta(venda);

        Produto produto = produtoRepository.findByIdAndTenantId(request.getProdutoId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", request.getProdutoId()));

        if (!produto.isAtivo()) {
            throw new BusinessException("Produto '" + produto.getNome() + "' está inativo");
        }

        if (produto.estoqueInsuficiente(request.getQuantidade())) {
            throw new BusinessException(
                    String.format("Estoque insuficiente para '%s'. Disponível: %s",
                            produto.getNome(), produto.getEstoqueAtual()));
        }

        BigDecimal descontoItem = request.getDescontoItem() != null
                ? request.getDescontoItem() : BigDecimal.ZERO;

        // Verifica se operador pode dar esse desconto
        validarDescontoPermitido(operadorId, tenantId, descontoItem,
                produto.getPrecoVenda(), request.getQuantidade());

        // Verifica se já existe o mesmo produto — se sim, soma quantidade
        venda.getItens().stream()
                .filter(i -> !i.isCancelado() && i.getProduto().getId().equals(produto.getId()))
                .findFirst()
                .ifPresentOrElse(
                        itemExistente -> {
                            itemExistente.setQuantidade(
                                    itemExistente.getQuantidade().add(request.getQuantidade()));
                            // Adiciona o desconto do novo lançamento ao item existente
                            itemExistente.setDescontoItem(
                                    itemExistente.getDescontoItem().add(descontoItem));
                            // Recalcula: precoUnit * qtdTotal - descontoTotal
                            BigDecimal bruto = itemExistente.getPrecoUnitario()
                                    .multiply(itemExistente.getQuantidade());
                            itemExistente.setTotalItem(bruto.subtract(itemExistente.getDescontoItem()));
                        },
                        () -> {
                            BigDecimal total = produto.getPrecoVenda()
                                    .multiply(request.getQuantidade())
                                    .subtract(descontoItem);

                            ItemVenda item = ItemVenda.builder()
                                    .venda(venda)
                                    .produto(produto)
                                    .nomeProduto(produto.getNome())
                                    .codigoBarras(produto.getCodigoBarras())
                                    .quantidade(request.getQuantidade())
                                    .precoUnitario(produto.getPrecoVenda())
                                    .descontoItem(descontoItem)
                                    .totalItem(total)
                                    .cancelado(false)
                                    .build();
                            venda.getItens().add(item);
                        }
                );

        recalcularTotaisVenda(venda);
        Venda salva = vendaRepository.save(venda);
        return toResponse(salva);
    }

    // =====================================================================
    // REMOVER / CANCELAR ITEM
    // =====================================================================

    @Transactional
    public VendaDTOs.Response cancelarItem(UUID vendaId, UUID itemId, UUID tenantId,
                                            VendaDTOs.CancelarItemRequest request) {
        Venda venda = vendaRepository.findByIdComItens(vendaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", vendaId));

        validarVendaAberta(venda);

        ItemVenda item = venda.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item de venda", itemId));

        if (item.isCancelado()) {
            throw new BusinessException("Item já está cancelado");
        }

        item.setCancelado(true);
        recalcularTotaisVenda(venda);
        Venda salva = vendaRepository.save(venda);

        log.info("Item cancelado: {} | Venda: {} | Motivo: {}",
                item.getNomeProduto(), venda.getNumeroVenda(), request.getMotivo());

        return toResponse(salva);
    }

    // =====================================================================
    // APLICAR DESCONTO NA VENDA
    // =====================================================================

    @Transactional
    public VendaDTOs.Response aplicarDesconto(UUID vendaId, UUID tenantId, UUID operadorId,
                                               VendaDTOs.AplicarDescontoRequest request) {
        Venda venda = vendaRepository.findByIdComItens(vendaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", vendaId));

        validarVendaAberta(venda);

        BigDecimal subtotal = venda.calcularSubtotal();
        BigDecimal desconto;

        if (request.isPercentual()) {
            if (request.getDesconto().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Percentual de desconto não pode ultrapassar 100%");
            }
            desconto = subtotal.multiply(request.getDesconto())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            desconto = request.getDesconto();
        }

        if (desconto.compareTo(subtotal) > 0) {
            throw new BusinessException("Desconto não pode ser maior que o subtotal da venda");
        }

        // Valida se operador tem permissão para o percentual de desconto
        BigDecimal percentual = subtotal.compareTo(BigDecimal.ZERO) > 0
                ? desconto.divide(subtotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        if (percentual.compareTo(BigDecimal.valueOf(operador.getDescontoMax())) > 0) {
            throw new BusinessException(
                    String.format("Desconto de %.2f%% excede o limite do operador (%.2f%%)",
                            percentual, operador.getDescontoMax()));
        }

        venda.setDesconto(desconto);
        recalcularTotaisVenda(venda);
        return toResponse(vendaRepository.save(venda));
    }

    // =====================================================================
    // FINALIZAR VENDA
    // =====================================================================

    @Transactional
    public VendaDTOs.Response finalizar(UUID vendaId, UUID tenantId, UUID operadorId,
                                         VendaDTOs.FinalizarRequest request) {
        Venda venda = vendaRepository.findByIdComItens(vendaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", vendaId));

        validarVendaAberta(venda);

        if (venda.getItens().stream().allMatch(ItemVenda::isCancelado)) {
            throw new BusinessException(
                    "Não é possível finalizar uma venda sem itens ativos");
        }

        BigDecimal totalVenda = venda.calcularTotal();

        // Valida pagamentos
        BigDecimal totalPagamentos = request.getPagamentos().stream()
                .map(VendaDTOs.PagamentoRequest::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagamentos.compareTo(totalVenda) < 0) {
            throw new BusinessException(
                    String.format("Valor pago (R$ %.2f) insuficiente para o total da venda (R$ %.2f)",
                            totalPagamentos, totalVenda));
        }

        // Valida fiado: exige cliente e limite
        for (VendaDTOs.PagamentoRequest pag : request.getPagamentos()) {
            if (pag.getForma() == FormaPagamento.FIADO) {
                if (venda.getCliente() == null) {
                    throw new BusinessException(
                            "Pagamento via fiado exige cliente selecionado na venda");
                }
                if (!venda.getCliente().possuiCreditoSuficiente(pag.getValor())) {
                    throw new BusinessException(
                            String.format("Limite de crédito insuficiente para fiado. " +
                                    "Disponível: R$ %.2f | Necessário: R$ %.2f",
                                    venda.getCliente().getCreditoDisponivel(), pag.getValor()));
                }
            }
        }

        // Baixa estoque de cada item ativo
        for (ItemVenda item : venda.getItens()) {
            if (!item.isCancelado()) {
                estoqueService.baixarEstoqueVenda(
                        tenantId, operadorId, vendaId,
                        item.getProduto().getId(), item.getQuantidade());
            }
        }

        // Registra pagamentos e atualiza totais do caixa
        BigDecimal trocoTotal = totalPagamentos.subtract(totalVenda);
        BigDecimal trocoRestante = trocoTotal;

        for (VendaDTOs.PagamentoRequest pag : request.getPagamentos()) {
            BigDecimal troco = BigDecimal.ZERO;

            // Troco apenas para dinheiro
            if (pag.getForma() == FormaPagamento.DINHEIRO && trocoRestante.compareTo(BigDecimal.ZERO) > 0) {
                troco = trocoRestante.min(pag.getValor().subtract(totalVenda).max(BigDecimal.ZERO));
                trocoRestante = trocoRestante.subtract(troco);
            }

            PagamentoVenda pagamento = PagamentoVenda.builder()
                    .venda(venda)
                    .forma(pag.getForma())
                    .valor(pag.getValor())
                    .troco(troco)
                    .status(StatusPagamento.CONFIRMADO)
                    .nsu(pag.getNsu())
                    .autorizacao(pag.getAutorizacao())
                    .build();

            venda.getPagamentos().add(pagamento);

            // Atualiza totais do caixa
            caixaService.adicionarTotalVenda(venda.getCaixa().getId(), pag.getForma(), pag.getValor());

            // Debita crédito de fiado no cliente
            if (pag.getForma() == FormaPagamento.FIADO) {
                clienteService.adicionarSaldoDevedor(
                        venda.getCliente().getId(), tenantId, pag.getValor());
            }
        }

        venda.setStatus(StatusVenda.FINALIZADA);
        venda.setSubtotal(venda.calcularSubtotal());
        venda.setTotal(totalVenda);

        Venda salva = vendaRepository.save(venda);

        log.info("Venda finalizada: {} | Total: R$ {} | Formas: {}",
                salva.getNumeroVenda(), totalVenda,
                request.getPagamentos().stream()
                        .map(p -> p.getForma().name())
                        .collect(Collectors.joining(", ")));

        return toResponse(salva);
    }

    // =====================================================================
    // CANCELAR VENDA
    // =====================================================================

    @Transactional
    public VendaDTOs.Response cancelar(UUID vendaId, UUID tenantId, UUID operadorId,
                                        VendaDTOs.CancelarRequest request) {
        Venda venda = vendaRepository.findByIdComItens(vendaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", vendaId));

        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new BusinessException("Venda já está cancelada");
        }

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        // Apenas GERENTE/ADMIN pode cancelar
        if (!operador.getRole().name().equals("GERENTE")
                && !operador.getRole().name().equals("ADMIN")) {
            throw new BusinessException(
                    "Apenas Gerente ou Administrador pode cancelar vendas");
        }

        // Se estava FINALIZADA, repõe estoque e reverte totais do caixa
        if (venda.getStatus() == StatusVenda.FINALIZADA) {
            for (ItemVenda item : venda.getItens()) {
                if (!item.isCancelado()) {
                    estoqueService.reporEstoqueCancelamento(
                            tenantId, operadorId, vendaId,
                            item.getProduto().getId(), item.getQuantidade());
                }
            }

            for (PagamentoVenda pag : venda.getPagamentos()) {
                pag.setStatus(StatusPagamento.ESTORNADO);
                caixaService.removerTotalVenda(
                        venda.getCaixa().getId(), pag.getForma(), pag.getValor());

                if (pag.getForma() == FormaPagamento.FIADO && venda.getCliente() != null) {
                    clienteService.estornarFiado(
                            venda.getCliente().getId(), tenantId, pag.getValor());
                }
            }
        }

        venda.setStatus(StatusVenda.CANCELADA);
        venda.setMotivoCancelamento(request.getMotivo());
        venda.setCanceladoPor(operador);
        venda.setDtCancelamento(LocalDateTime.now());

        Venda salva = vendaRepository.save(venda);

        log.info("Venda cancelada: {} | Operador: {} | Motivo: {}",
                salva.getNumeroVenda(), operador.getNome(), request.getMotivo());

        return toResponse(salva);
    }

    // =====================================================================
    // CONSULTAS
    // =====================================================================

    @Transactional(readOnly = true)
    public VendaDTOs.Response buscarPorId(UUID vendaId, UUID tenantId) {
        Venda venda = vendaRepository.findByIdComItens(vendaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda", vendaId));
        return toResponse(venda);
    }

    @Transactional(readOnly = true)
    public VendaDTOs.Response buscarPorNumero(String numero, UUID tenantId) {
        Venda venda = vendaRepository.findByNumeroVendaAndTenantId(numero, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venda não encontrada com número: " + numero));
        return toResponse(venda);
    }

    @Transactional(readOnly = true)
    public PageResponse<VendaDTOs.ResumoResponse> listar(UUID tenantId, UUID caixaId,
                                                           UUID pdvId, UUID operadorId,
                                                           UUID clienteId, StatusVenda status,
                                                           LocalDateTime inicio, LocalDateTime fim,
                                                           Pageable pageable) {
        Page<Venda> page = vendaRepository.findByFiltros(
                tenantId, caixaId, pdvId, operadorId, clienteId, status, inicio, fim, pageable);
        return PageResponse.of(page.map(this::toResumoResponse));
    }

    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private void validarVendaAberta(Venda venda) {
        if (venda.getStatus() != StatusVenda.ABERTA) {
            throw new BusinessException(
                    "Operação não permitida. Status atual da venda: " + venda.getStatus());
        }
    }

    private void validarDescontoPermitido(UUID operadorId, UUID tenantId,
                                           BigDecimal desconto, BigDecimal precoUnit,
                                           BigDecimal quantidade) {
        if (desconto.compareTo(BigDecimal.ZERO) == 0) return;

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        BigDecimal totalBruto = precoUnit.multiply(quantidade);
        if (totalBruto.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal percentual = desconto.divide(totalBruto, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (percentual.compareTo(BigDecimal.valueOf(operador.getDescontoMax())) > 0) {
            throw new BusinessException(
                    String.format("Desconto de %.2f%% excede o limite do operador (%.2f%%)",
                            percentual, operador.getDescontoMax()));
        }
    }

    private void recalcularTotaisVenda(Venda venda) {
        BigDecimal subtotal = venda.calcularSubtotal();
        venda.setSubtotal(subtotal);

        // Garante que desconto não ultrapasse subtotal
        if (venda.getDesconto().compareTo(subtotal) > 0) {
            venda.setDesconto(subtotal);
        }

        venda.setTotal(subtotal.subtract(venda.getDesconto()));
    }

    // =====================================================================
    // MAPEAMENTO
    // =====================================================================

    public VendaDTOs.Response toResponse(Venda venda) {
        VendaDTOs.Response r = new VendaDTOs.Response();
        r.setId(venda.getId());
        r.setNumeroVenda(venda.getNumeroVenda());
        r.setTenantId(venda.getTenant().getId());

        VendaDTOs.CaixaResumo caixaR = new VendaDTOs.CaixaResumo();
        caixaR.setId(venda.getCaixa().getId());
        r.setCaixa(caixaR);

        VendaDTOs.PDVResumo pdvR = new VendaDTOs.PDVResumo();
        pdvR.setId(venda.getPdv().getId());
        pdvR.setNome(venda.getPdv().getNome());
        r.setPdv(pdvR);

        VendaDTOs.OperadorResumo opR = new VendaDTOs.OperadorResumo();
        opR.setId(venda.getOperador().getId());
        opR.setNome(venda.getOperador().getNome());
        r.setOperador(opR);

        if (venda.getCliente() != null) {
            VendaDTOs.ClienteResumo cliR = new VendaDTOs.ClienteResumo();
            cliR.setId(venda.getCliente().getId());
            cliR.setNome(venda.getCliente().getNome());
            cliR.setCpfCnpj(venda.getCliente().getCpfCnpj());
            r.setCliente(cliR);
        }

        r.setDtVenda(venda.getDtVenda());
        r.setSubtotal(venda.getSubtotal());
        r.setDesconto(venda.getDesconto());
        r.setTotal(venda.getTotal());
        r.setTotalPago(venda.getTotalPago());
        r.setTroco(venda.getTroco());
        r.setStatus(venda.getStatus());
        r.setMotivoCancelamento(venda.getMotivoCancelamento());
        r.setDtCancelamento(venda.getDtCancelamento());
        r.setSincronizado(venda.isSincronizado());
        r.setCriadoEm(venda.getCriadoEm());

        r.setItens(venda.getItens().stream().map(i -> {
            VendaDTOs.ItemVendaResponse ir = new VendaDTOs.ItemVendaResponse();
            ir.setId(i.getId());
            ir.setProdutoId(i.getProduto().getId());
            ir.setNomeProduto(i.getNomeProduto());
            ir.setCodigoBarras(i.getCodigoBarras());
            ir.setCodigoProduto(i.getCodigoBarras()); // alias para compatibilidade frontend
            ir.setQuantidade(i.getQuantidade());
            ir.setPrecoUnitario(i.getPrecoUnitario());
            ir.setDescontoItem(i.getDescontoItem());
            ir.setTotalItem(i.getTotalItem());
            ir.setCancelado(i.isCancelado());
            return ir;
        }).collect(Collectors.toList()));

        r.setPagamentos(venda.getPagamentos().stream().map(p -> {
            VendaDTOs.PagamentoResponse pr = new VendaDTOs.PagamentoResponse();
            pr.setId(p.getId());
            pr.setForma(p.getForma());
            pr.setValor(p.getValor());
            pr.setTroco(p.getTroco());
            pr.setStatus(p.getStatus());
            pr.setNsu(p.getNsu());
            pr.setAutorizacao(p.getAutorizacao());
            return pr;
        }).collect(Collectors.toList()));

        return r;
    }

    private VendaDTOs.ResumoResponse toResumoResponse(Venda venda) {
        VendaDTOs.ResumoResponse r = new VendaDTOs.ResumoResponse();
        r.setId(venda.getId());
        r.setNumeroVenda(venda.getNumeroVenda());
        r.setDtVenda(venda.getDtVenda());
        r.setTotal(venda.getTotal());
        r.setStatus(venda.getStatus());
        r.setNomeCliente(venda.getCliente() != null ? venda.getCliente().getNome() : null);
        r.setNomeOperador(venda.getOperador().getNome());
        return r;
    }
}