package com.pdv.modules.caixa.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pdv.modules.caixa.dto.CaixaDTOs;
import com.pdv.modules.caixa.entity.Caixa;
import com.pdv.modules.caixa.entity.MovimentacaoCaixa;
import com.pdv.modules.caixa.entity.StatusCaixa;
import com.pdv.modules.caixa.entity.TipoMovimentacaoCaixa;
import com.pdv.modules.caixa.repository.CaixaRepository;
import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.pdv.repository.PDVRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.usuario.entity.Usuario;
import com.pdv.modules.usuario.repository.UsuarioRepository;
import com.pdv.modules.venda.entity.FormaPagamento;
import com.pdv.shared.dto.PageResponse;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final PDVRepository pdvRepository;
    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public CaixaDTOs.Response abrir(UUID tenantId, UUID operadorId, CaixaDTOs.AbrirRequest request) {
        PDV pdv = pdvRepository.findByIdAndTenantId(request.getPdvId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("PDV", request.getPdvId()));

        if (!pdv.isAtivo()) {
            throw new BusinessException("PDV inativo. Não é possível abrir caixa");
        }

        // Validação explícita de valor negativo no service
        if (request.getValorInicial() == null || request.getValorInicial().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor inicial do caixa não pode ser negativo");
        }

        // Verifica se já existe caixa ABERTO para este PDV
        caixaRepository.findByPdvIdAndStatus(pdv.getId(), StatusCaixa.ABERTO)
                .ifPresent(c -> {
                    throw new BusinessException(
                            "Já existe um caixa aberto para o PDV '" + pdv.getNome() +
                            "'. Feche o caixa atual antes de abrir um novo");
                });

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        Caixa caixa = Caixa.builder()
                .tenant(tenant)
                .pdv(pdv)
                .operador(operador)
                .dtAbertura(LocalDateTime.now())
                .valorInicial(request.getValorInicial())
                .totalSangria(BigDecimal.ZERO)
                .totalSuprimento(BigDecimal.ZERO)
                .status(StatusCaixa.ABERTO)
                .build();

        Caixa salvo = caixaRepository.save(caixa);

        registrarMovimentacao(salvo, TipoMovimentacaoCaixa.ABERTURA,
                request.getValorInicial(), "Abertura de caixa", operador);

        log.info("Caixa aberto: {} | PDV: {} | Operador: {} | Valor inicial: {}",
                salvo.getId(), pdv.getNome(), operador.getNome(), request.getValorInicial());

        return toResponse(salvo);
    }

    @Transactional
    public CaixaDTOs.Response fechar(UUID caixaId, UUID tenantId, UUID operadorId,
                                      CaixaDTOs.FecharRequest request) {
        Caixa caixa = caixaRepository.findByIdAndTenantId(caixaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", caixaId));

        if (!caixa.isAberto()) {
            throw new BusinessException("Caixa já está fechado");
        }

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        caixa.setDtFechamento(LocalDateTime.now());
        caixa.setStatus(StatusCaixa.FECHADO);
        caixa.setObservacaoFechamento(request.getObservacao());

        Caixa salvo = caixaRepository.save(caixa);

        registrarMovimentacao(salvo, TipoMovimentacaoCaixa.FECHAMENTO,
                salvo.getTotalVendas(), "Fechamento de caixa", operador);

        log.info("Caixa fechado: {} | Total vendas: {} | Saldo dinheiro: {}",
                caixaId, salvo.getTotalVendas(), salvo.getSaldoDinheiro());

        return toResponse(salvo);
    }

    @Transactional
    public CaixaDTOs.MovimentacaoResponse sangria(UUID caixaId, UUID tenantId, UUID operadorId,
                                                    CaixaDTOs.SangriaSuprimentoRequest request) {
        Caixa caixa = caixaRepository.findByIdAndTenantId(caixaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", caixaId));

        if (!caixa.isAberto()) {
            throw new BusinessException("Caixa não está aberto para realizar sangria");
        }

        if (caixa.getSaldoDinheiro().compareTo(request.getValor()) < 0) {
            throw new BusinessException(
                    String.format("Saldo em dinheiro insuficiente para sangria. " +
                                  "Disponível: R$ %.2f | Solicitado: R$ %.2f",
                            caixa.getSaldoDinheiro(), request.getValor()));
        }

        caixa.setTotalSangria(caixa.getTotalSangria().add(request.getValor()));
        caixaRepository.save(caixa);

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        MovimentacaoCaixa mov = registrarMovimentacao(caixa, TipoMovimentacaoCaixa.SANGRIA,
                request.getValor(), request.getMotivo(), operador);

        log.info("Sangria realizada: R$ {} | Caixa: {} | Operador: {}",
                request.getValor(), caixaId, operador.getNome());

        return toMovimentacaoResponse(mov);
    }

    @Transactional
    public CaixaDTOs.MovimentacaoResponse suprimento(UUID caixaId, UUID tenantId, UUID operadorId,
                                                       CaixaDTOs.SangriaSuprimentoRequest request) {
        Caixa caixa = caixaRepository.findByIdAndTenantId(caixaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", caixaId));

        if (!caixa.isAberto()) {
            throw new BusinessException("Caixa não está aberto para realizar suprimento");
        }

        caixa.setTotalSuprimento(caixa.getTotalSuprimento().add(request.getValor()));
        caixaRepository.save(caixa);

        Usuario operador = usuarioRepository.findByIdAndTenantId(operadorId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", operadorId));

        MovimentacaoCaixa mov = registrarMovimentacao(caixa, TipoMovimentacaoCaixa.SUPRIMENTO,
                request.getValor(), request.getMotivo(), operador);

        log.info("Suprimento realizado: R$ {} | Caixa: {} | Operador: {}",
                request.getValor(), caixaId, operador.getNome());

        return toMovimentacaoResponse(mov);
    }

    @Transactional(readOnly = true)
    public CaixaDTOs.Response buscarPorId(UUID caixaId, UUID tenantId) {
        Caixa caixa = caixaRepository.findByIdAndTenantId(caixaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", caixaId));
        return toResponse(caixa);
    }

    @Transactional(readOnly = true)
    public Optional<CaixaDTOs.Response> buscarMeuCaixaAberto(UUID operadorId, UUID tenantId) {
        return caixaRepository.findByOperadorIdAndStatus(operadorId, StatusCaixa.ABERTO)
                .filter(c -> c.getTenant().getId().equals(tenantId))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<CaixaDTOs.Response> buscarCaixaAbertoPorPdv(UUID pdvId, UUID tenantId) {
        return caixaRepository.findByPdvIdAndStatus(pdvId, StatusCaixa.ABERTO)
                .filter(c -> c.getTenant().getId().equals(tenantId))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CaixaDTOs.ResumoResponse> listarCaixasAbertos(UUID tenantId) {
        return caixaRepository.findCaixasAbertos(tenantId)
                .stream()
                .map(this::toResumoResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CaixaDTOs.ResumoResponse> listar(UUID tenantId, UUID pdvId,
                                                           UUID operadorId, StatusCaixa status,
                                                           LocalDateTime inicio, LocalDateTime fim,
                                                           Pageable pageable) {
        Page<Caixa> page = caixaRepository.findByFiltros(
                tenantId, pdvId, operadorId, status, inicio, fim, pageable);
        return PageResponse.of(page.map(this::toResumoResponse));
    }

    /**
     * Atualiza os totais por forma de pagamento ao finalizar uma venda.
     * Chamado internamente pelo VendaService.
     */
    @Transactional
    public void adicionarTotalVenda(UUID caixaId, FormaPagamento forma, BigDecimal valor) {
        Caixa caixa = caixaRepository.findById(caixaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", caixaId));

        switch (forma) {
            case DINHEIRO -> caixa.setTotalDinheiro(
                    nvl(caixa.getTotalDinheiro()).add(valor));
            case CARTAO_DEBITO -> caixa.setTotalDebito(
                    nvl(caixa.getTotalDebito()).add(valor));
            case CARTAO_CREDITO -> caixa.setTotalCredito(
                    nvl(caixa.getTotalCredito()).add(valor));
            case PIX -> caixa.setTotalPix(
                    nvl(caixa.getTotalPix()).add(valor));
            case FIADO -> caixa.setTotalFiado(
                    nvl(caixa.getTotalFiado()).add(valor));
        }

        caixaRepository.save(caixa);
    }

    /**
     * Reverte totais de pagamento ao cancelar uma venda.
     */
    @Transactional
    public void removerTotalVenda(UUID caixaId, FormaPagamento forma, BigDecimal valor) {
        Caixa caixa = caixaRepository.findById(caixaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixa", caixaId));

        switch (forma) {
            case DINHEIRO -> caixa.setTotalDinheiro(
                    nvl(caixa.getTotalDinheiro()).subtract(valor).max(BigDecimal.ZERO));
            case CARTAO_DEBITO -> caixa.setTotalDebito(
                    nvl(caixa.getTotalDebito()).subtract(valor).max(BigDecimal.ZERO));
            case CARTAO_CREDITO -> caixa.setTotalCredito(
                    nvl(caixa.getTotalCredito()).subtract(valor).max(BigDecimal.ZERO));
            case PIX -> caixa.setTotalPix(
                    nvl(caixa.getTotalPix()).subtract(valor).max(BigDecimal.ZERO));
            case FIADO -> caixa.setTotalFiado(
                    nvl(caixa.getTotalFiado()).subtract(valor).max(BigDecimal.ZERO));
        }

        caixaRepository.save(caixa);
    }

    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private MovimentacaoCaixa registrarMovimentacao(Caixa caixa, TipoMovimentacaoCaixa tipo,
                                                     BigDecimal valor, String motivo,
                                                     Usuario usuario) {
        MovimentacaoCaixa mov = MovimentacaoCaixa.builder()
                .caixa(caixa)
                .tipo(tipo)
                .valor(valor)
                .motivo(motivo)
                .usuario(usuario)
                .build();
        caixa.getMovimentacoes().add(mov);
        return mov;
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private CaixaDTOs.Response toResponse(Caixa caixa) {
        CaixaDTOs.Response r = new CaixaDTOs.Response();
        r.setId(caixa.getId());
        r.setTenantId(caixa.getTenant().getId());
        r.setPdv(toPdvResumo(caixa.getPdv()));
        r.setOperador(toOperadorResumo(caixa.getOperador()));
        r.setDtAbertura(caixa.getDtAbertura());
        r.setDtFechamento(caixa.getDtFechamento());
        r.setValorInicial(caixa.getValorInicial());
        r.setTotalDinheiro(nvl(caixa.getTotalDinheiro()));
        r.setTotalDebito(nvl(caixa.getTotalDebito()));
        r.setTotalCredito(nvl(caixa.getTotalCredito()));
        r.setTotalPix(nvl(caixa.getTotalPix()));
        r.setTotalFiado(nvl(caixa.getTotalFiado()));
        r.setTotalSangria(caixa.getTotalSangria());
        r.setTotalSuprimento(caixa.getTotalSuprimento());
        r.setTotalVendas(caixa.getTotalVendas());
        r.setSaldoDinheiro(caixa.getSaldoDinheiro());
        r.setStatus(caixa.getStatus());
        r.setObservacaoFechamento(caixa.getObservacaoFechamento());
        r.setCriadoEm(caixa.getCriadoEm());
        return r;
    }

    private CaixaDTOs.ResumoResponse toResumoResponse(Caixa caixa) {
        CaixaDTOs.ResumoResponse r = new CaixaDTOs.ResumoResponse();
        r.setId(caixa.getId());
        r.setPdv(toPdvResumo(caixa.getPdv()));
        r.setOperador(toOperadorResumo(caixa.getOperador()));
        r.setDtAbertura(caixa.getDtAbertura());
        r.setDtFechamento(caixa.getDtFechamento());
        r.setTotalVendas(caixa.getTotalVendas());
        r.setSaldoDinheiro(caixa.getSaldoDinheiro());
        r.setStatus(caixa.getStatus());
        return r;
    }

    private CaixaDTOs.MovimentacaoResponse toMovimentacaoResponse(MovimentacaoCaixa mov) {
        CaixaDTOs.MovimentacaoResponse r = new CaixaDTOs.MovimentacaoResponse();
        r.setId(mov.getId());
        r.setTipo(mov.getTipo());
        r.setValor(mov.getValor());
        r.setMotivo(mov.getMotivo());
        r.setNomeOperador(mov.getUsuario().getNome());
        r.setCriadoEm(mov.getCriadoEm());
        return r;
    }

    private CaixaDTOs.PDVResumo toPdvResumo(PDV pdv) {
        CaixaDTOs.PDVResumo r = new CaixaDTOs.PDVResumo();
        r.setId(pdv.getId());
        r.setNome(pdv.getNome());
        r.setDeviceId(pdv.getDeviceId());
        return r;
    }

    private CaixaDTOs.OperadorResumo toOperadorResumo(Usuario u) {
        CaixaDTOs.OperadorResumo r = new CaixaDTOs.OperadorResumo();
        r.setId(u.getId());
        r.setNome(u.getNome());
        return r;
    }
}