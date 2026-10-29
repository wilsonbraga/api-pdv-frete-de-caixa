package com.pdv.modules.cliente.service;

import com.pdv.modules.cliente.dto.ClienteDTOs;
import com.pdv.modules.cliente.dto.ClienteMapper;
import com.pdv.modules.cliente.entity.Cliente;
import com.pdv.modules.cliente.repository.ClienteRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final TenantRepository tenantRepository;
    private final ClienteMapper clienteMapper;

    @Transactional
    public ClienteDTOs.Response criar(UUID tenantId, ClienteDTOs.CreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (request.getCpfCnpj() != null && !request.getCpfCnpj().isBlank()) {
            if (clienteRepository.existsByCpfCnpjAndTenantId(request.getCpfCnpj(), tenantId)) {
                throw new BusinessException("Já existe cliente com CPF/CNPJ: " + request.getCpfCnpj());
            }
        }

        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setTenant(tenant);
        Cliente salvo = clienteRepository.save(cliente);
        log.info("Cliente criado: {} ({}) no tenant: {}", salvo.getNome(), salvo.getId(), tenantId);
        return toResponseComCredito(salvo);
    }

    @Transactional(readOnly = true)
    public ClienteDTOs.Response buscarPorId(UUID id, UUID tenantId) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        return toResponseComCredito(cliente);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClienteDTOs.ResumoResponse> listar(UUID tenantId, String nome,
                                                             String cpfCnpj, Boolean ativo,
                                                             Boolean comSaldoDevedor,
                                                             Pageable pageable) {
        var page = clienteRepository.findByFiltros(tenantId, nome, cpfCnpj, ativo,
                comSaldoDevedor, pageable);
        return PageResponse.of(page.map(clienteMapper::toResumoResponse));
    }

    @Transactional
    public ClienteDTOs.Response atualizar(UUID id, UUID tenantId, ClienteDTOs.UpdateRequest request) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        clienteMapper.updateFromRequest(request, cliente);
        return toResponseComCredito(clienteRepository.save(cliente));
    }

    @Transactional
    public void ativar(UUID id, UUID tenantId) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        cliente.setAtivo(true);
        clienteRepository.save(cliente);
    }

    @Transactional
    public void desativar(UUID id, UUID tenantId) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    /**
     * Adiciona saldo devedor (fiado). Chamado pelo VendaService ao fechar venda com fiado.
     */
    @Transactional
    public void adicionarSaldoDevedor(UUID id, UUID tenantId, BigDecimal valor) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        if (!cliente.possuiCreditoSuficiente(valor)) {
            throw new BusinessException(
                    String.format("Limite de crédito insuficiente. Disponível: R$ %.2f | Necessário: R$ %.2f",
                            cliente.getCreditoDisponivel(), valor));
        }
        cliente.setSaldoDevedor(cliente.getSaldoDevedor().add(valor));
        clienteRepository.save(cliente);
        log.info("Saldo devedor adicionado: R$ {} para cliente: {}", valor, id);
    }

    /**
     * Recebe pagamento de fiado — reduz saldo devedor.
     */
    @Transactional
    public ClienteDTOs.Response receberFiado(UUID id, UUID tenantId,
                                              ClienteDTOs.AtualizarSaldoRequest request) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));

        if (request.getValor().compareTo(cliente.getSaldoDevedor()) > 0) {
            throw new BusinessException(
                    String.format("Valor a receber (R$ %.2f) maior que o saldo devedor (R$ %.2f)",
                            request.getValor(), cliente.getSaldoDevedor()));
        }

        cliente.setSaldoDevedor(cliente.getSaldoDevedor().subtract(request.getValor()));
        clienteRepository.save(cliente);
        log.info("Fiado recebido: R$ {} para cliente: {}", request.getValor(), id);
        return toResponseComCredito(cliente);
    }

    /**
     * Estorna fiado — recoloca o valor no saldo devedor após cancelamento de venda.
     */
    @Transactional
    public void estornarFiado(UUID id, UUID tenantId, BigDecimal valor) {
        Cliente cliente = clienteRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        // No cancelamento, o saldo devedor cai (era devedor, cancelou a venda)
        BigDecimal novoSaldo = cliente.getSaldoDevedor().subtract(valor);
        cliente.setSaldoDevedor(novoSaldo.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO : novoSaldo);
        clienteRepository.save(cliente);
    }

    private ClienteDTOs.Response toResponseComCredito(Cliente cliente) {
        ClienteDTOs.Response r = clienteMapper.toResponse(cliente);
        r.setCreditoDisponivel(cliente.getCreditoDisponivel());
        return r;
    }
}
