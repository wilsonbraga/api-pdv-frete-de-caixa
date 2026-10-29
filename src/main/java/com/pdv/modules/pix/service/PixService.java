package com.pdv.modules.pix.service;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.pdv.config.PdvProperties;
import com.pdv.config.RabbitMQConfig;
import com.pdv.modules.pix.dto.PixDTOs;
import com.pdv.modules.pix.entity.*;
import com.pdv.modules.pix.repository.CobrancaPixRepository;
import com.pdv.modules.pix.repository.ConfiguracaoPixRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.modules.venda.entity.Venda;
import com.pdv.modules.venda.repository.VendaRepository;
import com.pdv.shared.exception.BusinessException;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PixService {

    private final ConfiguracaoPixRepository configRepository;
    private final CobrancaPixRepository     cobrancaRepository;
    private final TenantRepository          tenantRepository;
    private final VendaRepository           vendaRepository;
    private final MercadoPagoService        mpService;
    private final RabbitTemplate            rabbitTemplate;
    private final SimpMessagingTemplate     wsTemplate;
    private final PdvProperties             pdvProperties;

    // =====================================================================
    // CONFIGURAÇÃO PIX
    // =====================================================================

    @Transactional
    public PixDTOs.ConfiguracaoResponse salvarConfiguracao(UUID tenantId,
                                                            PixDTOs.ConfiguracaoCreateRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        if (configRepository.existsByTenantIdAndProvedor(tenantId, request.getProvedor())) {
            throw new BusinessException(
                    "Já existe uma configuração " + request.getProvedor().name() +
                    " para este tenant. Use a edição para atualizar.");
        }

        // Validar credencial antes de salvar — evita salvar token inválido
        if (!mpService.validarCredencial(request.getMpAccessToken())) {
            throw new BusinessException(
                    "Access Token do Mercado Pago inválido. " +
                    "Verifique suas credenciais em: Mercado Pago Developers → Credenciais.");
        }

        // Fallbacks para campos opcionais
        String nomeBeneficiario = (request.getNomeBeneficiario() != null
                && !request.getNomeBeneficiario().isBlank())
                ? request.getNomeBeneficiario()
                : request.getChavePix();

        String tipoChave = (request.getTipoChave() != null
                && !request.getTipoChave().isBlank())
                ? request.getTipoChave()
                : detectarTipoChave(request.getChavePix());

        ConfiguracaoPix config = ConfiguracaoPix.builder()
                .tenant(tenant)
                .provedor(request.getProvedor())
                .ativo(true)
                .mpAccessToken(request.getMpAccessToken())
                .mpPublicKey(request.getMpPublicKey())
                .chavePix(request.getChavePix())
                .tipoChave(tipoChave)
                .nomeBeneficiario(nomeBeneficiario)
                .descricaoPadrao(request.getDescricaoPadrao() != null
                        && !request.getDescricaoPadrao().isBlank()
                        ? request.getDescricaoPadrao()
                        : "Pagamento PDV")
                .sandbox(request.isSandbox())
                .build();

        ConfiguracaoPix salva = configRepository.save(config);
        log.info("Configuração PIX {} cadastrada para tenant: {}", request.getProvedor(), tenantId);
        return toConfigResponse(salva);
    }

    @Transactional
    public PixDTOs.ConfiguracaoResponse atualizarConfiguracao(UUID id, UUID tenantId,
                                                               PixDTOs.ConfiguracaoUpdateRequest request) {
        ConfiguracaoPix config = configRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração PIX", id));

        // Validar novo token se foi informado
        if (request.getMpAccessToken() != null && !request.getMpAccessToken().isBlank()) {
            if (!mpService.validarCredencial(request.getMpAccessToken())) {
                throw new BusinessException(
                        "Novo Access Token do Mercado Pago inválido. Verifique suas credenciais.");
            }
            config.setMpAccessToken(request.getMpAccessToken());
        }

        if (request.getMpPublicKey()      != null) config.setMpPublicKey(request.getMpPublicKey());
        if (request.getChavePix()         != null) config.setChavePix(request.getChavePix());
        if (request.getTipoChave()        != null) config.setTipoChave(request.getTipoChave());
        if (request.getNomeBeneficiario() != null) config.setNomeBeneficiario(request.getNomeBeneficiario());
        if (request.getDescricaoPadrao()  != null) config.setDescricaoPadrao(request.getDescricaoPadrao());
        if (request.getSandbox()          != null) config.setSandbox(request.getSandbox());
        if (request.getAtivo()            != null) config.setAtivo(request.getAtivo());

        return toConfigResponse(configRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<PixDTOs.ConfiguracaoResponse> listarConfiguracoes(UUID tenantId) {
        return configRepository.findByTenantIdAndAtivoTrue(tenantId)
                .stream()
                .map(this::toConfigResponse)
                .collect(Collectors.toList());
    }

    // =====================================================================
    // LISTAR COBRANÇAS (histórico com filtros e paginação)
    // =====================================================================

    @Transactional(readOnly = true)
    public Page<PixDTOs.CobrancaResponse> listarCobrancas(UUID tenantId,
                                                            PixDTOs.CobrancaFiltroRequest filtro,
                                                            int page, int size) {
        LocalDateTime dataInicio = filtro.getDataInicio() != null
                ? filtro.getDataInicio().atStartOfDay() : null;
        LocalDateTime dataFim = filtro.getDataFim() != null
                ? filtro.getDataFim().atTime(23, 59, 59) : null;

        return cobrancaRepository.listarPorFiltros(
                tenantId,
                filtro.getStatus(),
                dataInicio,
                dataFim,
                PageRequest.of(page, Math.min(size, 50))
        ).map(this::toCobrancaResponse);
    }

    // =====================================================================
    // GERAR COBRANÇA PIX
    // =====================================================================

    @Transactional
    public PixDTOs.CobrancaResponse gerarCobranca(UUID tenantId,
                                                    PixDTOs.GerarCobrancaRequest request) {
        ConfiguracaoPix config = resolverConfiguracao(tenantId, request.getConfiguracaoPixId());

        if (!config.isAtivo()) {
            throw new BusinessException("Configuração PIX está inativa. Ative-a em: PIX → Configuração.");
        }

        Venda venda = null;
        if (request.getVendaId() != null) {
            venda = vendaRepository.findByIdAndTenantId(request.getVendaId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Venda", request.getVendaId()));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        int expiracaoMinutos = pdvProperties.getMercadopago().getExpiracaoMinutos();

        // Gera ID antecipadamente para usar no externalReference do MP e no e-mail fictício
        UUID cobrancaId = UUID.randomUUID();

        try {
            MercadoPagoService.PixPaymentResult mpResult = mpService.gerarCobrancaPix(
                    config,
                    request.getValor(),
                    request.getDescricao(),
                    cobrancaId.toString(),   // externalReference = nosso ID
                    expiracaoMinutos);

            CobrancaPix cobranca = CobrancaPix.builder()
                    .id(cobrancaId)          // usa o ID pré-gerado
                    .tenant(tenant)
                    .venda(venda)
                    .configuracaoPix(config)
                    .mpPaymentId(mpResult.mpPaymentId())   // ID real do pagamento no MP
                    .valor(request.getValor())
                    .descricao(request.getDescricao() != null && !request.getDescricao().isBlank()
                            ? request.getDescricao()
                            : config.getDescricaoPadrao())
                    .qrCode(mpResult.qrCode())
                    .qrCodeBase64(mpResult.qrCodeBase64())
                    .ticketUrl(mpResult.ticketUrl())
                    .status(StatusCobrancaPix.PENDENTE)
                    .expiracao(LocalDateTime.now().plusMinutes(expiracaoMinutos))
                    .deviceId(request.getDeviceId())
                    .build();

            CobrancaPix salva = cobrancaRepository.save(cobranca);

            log.info("Cobrança PIX gerada: id={} | tenant={} | valor=R${} | mpPaymentId={}",
                    salva.getId(), tenantId, request.getValor(), mpResult.mpPaymentId());

            return toCobrancaResponse(salva);

        } catch (MPApiException e) {
            log.error("Erro API Mercado Pago ao gerar cobrança — HTTP {}: {}",
                    e.getStatusCode(),
                    e.getApiResponse() != null ? e.getApiResponse().getContent() : "sem body");
            throw new BusinessException(
                    "Erro ao gerar PIX no Mercado Pago (HTTP " + e.getStatusCode() + "). " +
                    "Verifique se o Access Token está correto e se a conta está ativa.");
        } catch (MPException e) {
            log.error("Erro de comunicação com Mercado Pago: {}", e.getMessage());
            throw new BusinessException(
                    "Erro de comunicação com o Mercado Pago. Verifique sua conexão e tente novamente.");
        }
    }

    // =====================================================================
    // CONSULTAR STATUS
    // =====================================================================

    @Transactional(readOnly = true)
    public PixDTOs.StatusCobrancaResponse consultarStatus(UUID cobrancaId, UUID tenantId) {
        CobrancaPix cobranca = cobrancaRepository.findByIdAndTenantId(cobrancaId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cobrança PIX", cobrancaId));

        // Verifica expiração na consulta — garante status atualizado mesmo sem o job rodar
        if (cobranca.isPendente() && cobranca.isExpirado()) {
            cobranca.setStatus(StatusCobrancaPix.EXPIRADO);
            cobrancaRepository.save(cobranca);
        }

        PixDTOs.StatusCobrancaResponse response = new PixDTOs.StatusCobrancaResponse();
        response.setId(cobranca.getId());
        response.setStatus(cobranca.getStatus());
        response.setPagoEm(cobranca.getPagoEm());
        response.setExpirado(cobranca.isExpirado());
        response.setMensagem(mensagemStatus(cobranca.getStatus()));
        return response;
    }

    // =====================================================================
    // WEBHOOK MERCADO PAGO
    // =====================================================================

    @Transactional
    public void processarWebhookMercadoPago(UUID tenantId,
                                             PixDTOs.MpWebhookRequest webhook,
                                             String payloadRaw) {
        log.info("Webhook MP — action={} | type={} | tenant={}",
                webhook.getAction(), webhook.getType(), tenantId);

        // Só processa eventos de pagamento
        if (!"payment".equals(webhook.getType())) return;

        if (webhook.getData() == null || webhook.getData().getId() == null) {
            log.warn("Webhook MP sem data.id — ignorado");
            return;
        }

        String mpPaymentId = webhook.getData().getId();

        CobrancaPix cobranca = cobrancaRepository.findByMpPaymentId(mpPaymentId).orElse(null);
        if (cobranca == null) {
            log.warn("Cobrança PIX não encontrada para mpPaymentId={}", mpPaymentId);
            return;
        }

        // Idempotência — ignora se já foi processado
        if (!cobranca.isPendente()) {
            log.info("Cobrança {} já está {} — webhook ignorado",
                    cobranca.getId(), cobranca.getStatus());
            return;
        }

        ConfiguracaoPix config = cobranca.getConfiguracaoPix();

        try {
            Payment payment = mpService.buscarPagamento(mpPaymentId, config.getMpAccessToken());
            String status = payment.getStatus();
            log.info("Status MP do pagamento {}: {}", mpPaymentId, status);

            switch (status) {
                case "approved" -> {
                    cobranca.setStatus(StatusCobrancaPix.PAGO);
                    cobranca.setPagoEm(LocalDateTime.now());
                    cobranca.setWebhookPayload(payloadRaw);
                    cobrancaRepository.save(cobranca);
                    publicarPixConfirmado(cobranca);
                    log.info("✅ PIX CONFIRMADO — cobrança={} | venda={} | valor=R${}",
                            cobranca.getId(),
                            cobranca.getVenda() != null ? cobranca.getVenda().getId() : "N/A",
                            cobranca.getValor());
                }
                case "cancelled", "rejected" -> {
                    cobranca.setStatus(StatusCobrancaPix.CANCELADO);
                    cobranca.setCanceladoEm(LocalDateTime.now());
                    cobrancaRepository.save(cobranca);
                    log.info("PIX cancelado/rejeitado — cobrança={} | statusMP={}", cobranca.getId(), status);
                }
                default -> log.debug("Status MP não processado: {} — aguardando aprovação", status);
            }

        } catch (MPApiException e) {
            log.error("Erro API ao verificar pagamento {} — HTTP {}: {}",
                    mpPaymentId, e.getStatusCode(),
                    e.getApiResponse() != null ? e.getApiResponse().getContent() : "null");
            cobranca.setErroMensagem("Erro API MP HTTP " + e.getStatusCode());
            cobrancaRepository.save(cobranca);
        } catch (MPException e) {
            log.error("Erro de comunicação ao verificar pagamento {}: {}", mpPaymentId, e.getMessage());
            cobranca.setErroMensagem(e.getMessage());
            cobrancaRepository.save(cobranca);
        }
    }

    // =====================================================================
    // JOB — expira cobranças vencidas a cada 2 minutos
    // =====================================================================

    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void expirarCobrancasVencidas() {
        int expiradas = cobrancaRepository.expirarCobrancasVencidas(LocalDateTime.now());
        if (expiradas > 0) {
            log.info("Cobranças PIX expiradas automaticamente: {}", expiradas);
        }
    }

    // =====================================================================
    // HELPERS PRIVADOS
    // =====================================================================

    private ConfiguracaoPix resolverConfiguracao(UUID tenantId, UUID configuracaoId) {
        if (configuracaoId != null) {
            return configRepository.findByIdAndTenantId(configuracaoId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Configuração PIX", configuracaoId));
        }

        List<ConfiguracaoPix> configs = configRepository.findByTenantIdAndAtivoTrue(tenantId);

        if (configs.isEmpty()) {
            throw new BusinessException(
                    "Nenhuma configuração PIX ativa encontrada. " +
                    "Configure o Mercado Pago em: POST /pix/configuracoes");
        }
        if (configs.size() == 1) return configs.get(0);

        throw new BusinessException(
                "Este tenant possui " + configs.size() + " configurações PIX ativas. " +
                "Informe o campo 'configuracaoPixId' para selecionar qual usar.");
    }

    /**
     * Detecta automaticamente o tipo da chave PIX pelo formato.
     */
    private String detectarTipoChave(String chave) {
        if (chave == null || chave.isBlank()) return "ALEATORIA";
        String c = chave.trim();
        if (c.matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}") || c.matches("\\d{11}")) return "CPF";
        if (c.matches("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}") || c.matches("\\d{14}")) return "CNPJ";
        if (c.startsWith("+")) return "TELEFONE";
        if (c.contains("@")) return "EMAIL";
        return "ALEATORIA";
    }

    private void publicarPixConfirmado(CobrancaPix cobranca) {
        try {
            PixDTOs.PixConfirmadoEvent event = new PixDTOs.PixConfirmadoEvent();
            event.setCobrancaPixId(cobranca.getId());
            event.setTenantId(cobranca.getTenant().getId());
            event.setVendaId(cobranca.getVenda() != null ? cobranca.getVenda().getId() : null);
            event.setMpPaymentId(cobranca.getMpPaymentId());
            event.setValor(cobranca.getValor());
            event.setPagoEm(cobranca.getPagoEm());

            // RabbitMQ — processa a venda de forma assíncrona
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_PDV,
                    RabbitMQConfig.ROUTING_PIX,
                    event);

            // WebSocket — notifica o PDV em tempo real
            wsTemplate.convertAndSend(
                    "/topic/pix/" + cobranca.getTenant().getId(),
                    event);

        } catch (Exception e) {
            // Não lança exceção — o PIX já foi confirmado, só o evento falhou
            log.error("Erro ao publicar evento PIX confirmado (cobrança={}) — {}",
                    cobranca.getId(), e.getMessage());
        }
    }

    private String mensagemStatus(StatusCobrancaPix status) {
        return switch (status) {
            case PENDENTE  -> "Aguardando pagamento";
            case PAGO      -> "Pagamento confirmado";
            case EXPIRADO  -> "QR Code expirado. Gere um novo";
            case CANCELADO -> "Cobrança cancelada";
            case ERRO      -> "Erro no processamento";
        };
    }

    private PixDTOs.ConfiguracaoResponse toConfigResponse(ConfiguracaoPix c) {
        PixDTOs.ConfiguracaoResponse r = new PixDTOs.ConfiguracaoResponse();
        r.setId(c.getId());
        r.setProvedor(c.getProvedor());
        r.setAtivo(c.isAtivo());
        r.setSandbox(c.isSandbox());
        // Nunca retorna o token completo — apenas os últimos 6 chars mascarados
        if (c.getMpAccessToken() != null && c.getMpAccessToken().length() > 6) {
            r.setMpAccessTokenMascarado("****" +
                    c.getMpAccessToken().substring(c.getMpAccessToken().length() - 6));
        }
        r.setMpPublicKey(c.getMpPublicKey());
        r.setMpUserId(c.getMpUserId());
        r.setChavePix(c.getChavePix());
        r.setTipoChave(c.getTipoChave());
        r.setNomeBeneficiario(c.getNomeBeneficiario());
        r.setDescricaoPadrao(c.getDescricaoPadrao());
        r.setCriadoEm(c.getCriadoEm());
        return r;
    }

    private PixDTOs.CobrancaResponse toCobrancaResponse(CobrancaPix c) {
        PixDTOs.CobrancaResponse r = new PixDTOs.CobrancaResponse();
        r.setId(c.getId());
        r.setValor(c.getValor());
        r.setDescricao(c.getDescricao());
        r.setQrCode(c.getQrCode());
        r.setQrCodeBase64(c.getQrCodeBase64());
        r.setTicketUrl(c.getTicketUrl());
        r.setStatus(c.getStatus());
        r.setExpiracao(c.getExpiracao());
        r.setPagoEm(c.getPagoEm());
        r.setProvedor(c.getConfiguracaoPix().getProvedor());
        r.setCriadoEm(c.getCriadoEm());
        return r;
    }
}
