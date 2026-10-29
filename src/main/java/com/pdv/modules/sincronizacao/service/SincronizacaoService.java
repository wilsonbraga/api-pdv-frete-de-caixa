package com.pdv.modules.sincronizacao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdv.config.PdvProperties;
import com.pdv.modules.pdv.entity.PDV;
import com.pdv.modules.pdv.repository.PDVRepository;
import com.pdv.modules.sincronizacao.dto.SincronizacaoDTOs;
import com.pdv.modules.sincronizacao.entity.*;
import com.pdv.modules.sincronizacao.repository.FilaSincronizacaoRepository;
import com.pdv.modules.tenant.entity.Tenant;
import com.pdv.modules.tenant.repository.TenantRepository;
import com.pdv.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de sincronização offline-first.
 *
 * Em desenvolvimento local sem RabbitMQ, as operações são processadas
 * de forma síncrona diretamente no endpoint REST, sem fila de mensagens.
 *
 * Quando RabbitMQ estiver disponível (produção), habilitar no application.yml:
 * spring.rabbitmq.enabled: true
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoService {

    private final FilaSincronizacaoRepository filaRepository;
    private final TenantRepository tenantRepository;
    private final PDVRepository pdvRepository;
    private final ObjectMapper objectMapper;
    private final PdvProperties pdvProperties;
    private final SimpMessagingTemplate wsTemplate;

    /**
     * Recebe um lote de operações offline de um PDV,
     * persiste na fila e processa imediatamente (sem RabbitMQ em dev).
     */
    @Transactional
    public SincronizacaoDTOs.LoteSyncResponse receberLote(UUID tenantId,
                                                            SincronizacaoDTOs.LoteSyncRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        PDV pdv = pdvRepository.findByDeviceIdAndTenantId(request.getDeviceId(), tenantId)
                .orElse(null);

        List<SincronizacaoDTOs.ItemSyncResponse> erros = new ArrayList<>();
        int sincronizados = 0;

        for (SincronizacaoDTOs.ItemSyncRequest item : request.getOperacoes()) {
            try {
                FilaSincronizacao registro = FilaSincronizacao.builder()
                        .tenant(tenant)
                        .pdv(pdv)
                        .entidade(item.getEntidade())
                        .operacao(item.getOperacao())
                        .payload(item.getPayload())
                        .status(StatusSync.PENDENTE)
                        .tentativas(0)
                        .deviceId(request.getDeviceId())
                        .build();

                FilaSincronizacao salvo = filaRepository.save(registro);

                // Processamento síncrono direto (sem RabbitMQ)
                processarItemSync(salvo);

                sincronizados++;
            } catch (Exception e) {
                log.error("Erro ao processar sync: entidade={} | erro={}",
                        item.getEntidade(), e.getMessage());

                SincronizacaoDTOs.ItemSyncResponse erro = new SincronizacaoDTOs.ItemSyncResponse();
                erro.setEntidade(item.getEntidade());
                erro.setOperacao(item.getOperacao());
                erro.setStatus(StatusSync.ERRO);
                erro.setErroMensagem(e.getMessage());
                erros.add(erro);
            }
        }

        log.info("Lote sync processado — tenant: {} | device: {} | ok: {} | erros: {}",
                tenantId, request.getDeviceId(), sincronizados, erros.size());

        SincronizacaoDTOs.LoteSyncResponse response = new SincronizacaoDTOs.LoteSyncResponse();
        response.setTotalRecebidos(request.getOperacoes().size());
        response.setTotalSincronizados(sincronizados);
        response.setTotalErros(erros.size());
        response.setErros(erros);
        return response;
    }

    /**
     * Processa um item da fila de sincronização.
     * Chamado diretamente (dev local) ou via RabbitMQ consumer (produção).
     */
    @Transactional
    public void processarItemSync(FilaSincronizacao item) {
        try {
            filaRepository.atualizarStatus(
                    item.getId(), StatusSync.SINCRONIZADO, null, LocalDateTime.now());

            broadcastSyncEvent(item);

            log.debug("Item sync processado: {} | {} | {}",
                    item.getId(), item.getEntidade(), item.getOperacao());
        } catch (Exception e) {
            int tentativas = item.getTentativas() + 1;
            StatusSync novoStatus = tentativas >= pdvProperties.getSync().getMaxRetries()
                    ? StatusSync.ERRO : StatusSync.PENDENTE;

            filaRepository.atualizarStatus(
                    item.getId(), novoStatus, e.getMessage(), LocalDateTime.now());

            log.error("Erro ao processar sync {}: {} (tentativa {}/{})",
                    item.getId(), e.getMessage(), tentativas,
                    pdvProperties.getSync().getMaxRetries());
        }
    }

    /**
     * Broadcast via WebSocket para todos os PDVs do tenant.
     */
    private void broadcastSyncEvent(FilaSincronizacao item) {
        try {
            SincronizacaoDTOs.SyncEventPayload event = new SincronizacaoDTOs.SyncEventPayload();
            event.setTipo(item.getEntidade().toUpperCase() + "_" + item.getOperacao().name());
            event.setTenantId(item.getTenant().getId());
            event.setEntidade(item.getEntidade());
            event.setPayload(item.getPayload());
            event.setOcorridoEm(LocalDateTime.now());

            wsTemplate.convertAndSend(
                    "/topic/sync/" + item.getTenant().getId().toString(), event);
        } catch (Exception e) {
            log.warn("Falha ao broadcast WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Status de sincronização por tenant.
     */
    @Transactional(readOnly = true)
    public SincronizacaoDTOs.StatusSyncResponse getStatus(UUID tenantId) {
        long pendentes = filaRepository.countPendentesByTenant(tenantId);
        SincronizacaoDTOs.StatusSyncResponse r = new SincronizacaoDTOs.StatusSyncResponse();
        r.setTenantId(tenantId);
        r.setTotalPendentes(pendentes);
        r.setSincronizado(pendentes == 0);
        return r;
    }

    /**
     * Reprocessa itens travados em PROCESSANDO — executa a cada 5 minutos.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void recuperarProcessandosTravados() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        int recuperados = filaRepository.recolocarProcessandoEmPendente(
                limite, pdvProperties.getSync().getMaxRetries());
        if (recuperados > 0) {
            log.info("Sync recovery: {} item(s) recolocado(s) em PENDENTE", recuperados);
        }
    }
}
