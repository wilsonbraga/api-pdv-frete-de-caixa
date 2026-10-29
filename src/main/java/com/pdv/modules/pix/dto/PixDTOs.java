package com.pdv.modules.pix.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.pdv.modules.pix.entity.ProvedorPix;
import com.pdv.modules.pix.entity.StatusCobrancaPix;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class PixDTOs {

    // =========================================================
    // CONFIGURAÇÃO
    // =========================================================

    @Data
    public static class ConfiguracaoCreateRequest {

        @NotNull(message = "Provedor é obrigatório")
        private ProvedorPix provedor;

        @NotBlank(message = "Access Token é obrigatório")
        private String mpAccessToken;

        // Opcional — usado apenas para checkout transparente
        private String mpPublicKey;

        @NotBlank(message = "Chave PIX é obrigatória")
        private String chavePix;

        // Opcional — detectado automaticamente pelo PixService se não informado
        private String tipoChave; // CPF, CNPJ, EMAIL, TELEFONE, ALEATORIA

        // Opcional — usa a própria chavePix como fallback se não informado
        private String nomeBeneficiario;

        // Opcional — padrão: "Pagamento PDV"
        private String descricaoPadrao;

        // true = sandbox/testes, false = produção real
        private boolean sandbox = true;
    }

    @Data
    public static class ConfiguracaoUpdateRequest {
        private String mpAccessToken;
        private String mpPublicKey;
        private String chavePix;
        private String tipoChave;
        private String nomeBeneficiario;
        private String descricaoPadrao;
        private Boolean sandbox;
        private Boolean ativo;
    }

    @Data
    public static class ConfiguracaoResponse {
        private UUID id;
        private ProvedorPix provedor;
        private boolean ativo;
        private boolean sandbox;
        // Nunca retorna o token completo — só os últimos 6 chars mascarados
        private String mpAccessTokenMascarado;
        private String mpPublicKey;
        private String mpUserId;
        private String chavePix;
        private String tipoChave;
        private String nomeBeneficiario;
        private String descricaoPadrao;
        private LocalDateTime criadoEm;
    }

    // =========================================================
    // COBRANÇA PIX
    // =========================================================

    @Data
    public static class GerarCobrancaRequest {

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        private BigDecimal valor;

        // Opcional — usa a descrição padrão da configuração se não informado
        private String descricao;

        // Opcional — vincula o PIX à venda para atualização automática
        private UUID vendaId;

        // Opcional — necessário apenas se o tenant tiver múltiplas configurações PIX
        private UUID configuracaoPixId;

        // Opcional — rastreia qual PDV/caixa gerou a cobrança
        private String deviceId;
    }

    @Data
    public static class CobrancaResponse {
        private UUID id;
        private BigDecimal valor;
        private String descricao;
        private String qrCode;         // Código copia e cola
        private String qrCodeBase64;   // Imagem base64 — exibida no PDV
        private String ticketUrl;      // Link para pagar no browser
        private StatusCobrancaPix status;
        private LocalDateTime expiracao;
        private LocalDateTime pagoEm;
        private ProvedorPix provedor;
        private LocalDateTime criadoEm;
    }

    @Data
    public static class StatusCobrancaResponse {
        private UUID id;
        private StatusCobrancaPix status;
        private LocalDateTime pagoEm;
        private boolean expirado;
        private String mensagem;
    }

    // =========================================================
    // WEBHOOK MERCADO PAGO
    // =========================================================

    @Data
    public static class MpWebhookRequest {
        private String action; // payment.created, payment.updated
        private String type;   // payment
        private MpWebhookData data;

        @Data
        public static class MpWebhookData {
            private String id; // ID do pagamento no MP
        }
    }

    // =========================================================
    // LISTAGEM / FILTROS (histórico de cobranças)
    // =========================================================

    @Data
    public static class CobrancaFiltroRequest {
        private StatusCobrancaPix status;
        private LocalDate dataInicio;
        private LocalDate dataFim;
    }

    // =========================================================
    // EVENTO RABBITMQ / WEBSOCKET
    // =========================================================

    @Data
    public static class PixConfirmadoEvent {
        private UUID cobrancaPixId;
        private UUID tenantId;
        private UUID vendaId;
        private String mpPaymentId;
        private BigDecimal valor;
        private LocalDateTime pagoEm;
    }
}
