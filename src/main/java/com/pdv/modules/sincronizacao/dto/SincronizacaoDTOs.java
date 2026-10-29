package com.pdv.modules.sincronizacao.dto;

import com.pdv.modules.sincronizacao.entity.OperacaoSync;
import com.pdv.modules.sincronizacao.entity.StatusSync;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SincronizacaoDTOs {

    @Data
    public static class ItemSyncRequest {
        @NotNull(message = "Entidade é obrigatória")
        private String entidade;

        @NotNull(message = "Operação é obrigatória")
        private OperacaoSync operacao;

        @NotBlank(message = "Payload é obrigatório")
        private String payload; // JSON serializado da entidade

        private String deviceId;
        private UUID pdvId;
        private LocalDateTime criadoEmDevice;
    }

    @Data
    public static class LoteSyncRequest {
        @NotEmpty(message = "A lista de operações não pode ser vazia")
        @Valid
        private List<ItemSyncRequest> operacoes;

        @NotBlank(message = "Device ID é obrigatório")
        private String deviceId;
    }

    @Data
    public static class ItemSyncResponse {
        private UUID id;
        private String entidade;
        private OperacaoSync operacao;
        private StatusSync status;
        private String erroMensagem;
        private int tentativas;
        private LocalDateTime criadoEm;
        private LocalDateTime sincronizadoEm;
    }

    @Data
    public static class LoteSyncResponse {
        private int totalRecebidos;
        private int totalSincronizados;
        private int totalErros;
        private List<ItemSyncResponse> erros;
    }

    @Data
    public static class StatusSyncResponse {
        private UUID tenantId;
        private long totalPendentes;
        private boolean sincronizado;
    }

    /** Evento WebSocket broadcast para os PDVs do tenant */
    @Data
    public static class SyncEventPayload {
        private String tipo; // PRODUTO_ATUALIZADO, VENDA_SINCRONIZADA, ESTOQUE_ATUALIZADO, etc.
        private UUID tenantId;
        private String entidade;
        private String payload;
        private LocalDateTime ocorridoEm;
    }
}
