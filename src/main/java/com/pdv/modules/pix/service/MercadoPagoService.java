package com.pdv.modules.pix.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.pdv.modules.pix.dto.PixDTOs;
import com.pdv.modules.pix.entity.ConfiguracaoPix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Integração com a API do Mercado Pago para geração de cobranças PIX.
 *
 * IMPORTANTE — por que CPF e e-mail fictícios:
 *  - O MP exige CPF válido no campo identification.number para cobranças PIX.
 *    Como o PDV não coleta CPF do cliente na frente de caixa, usamos um CPF
 *    padrão matematicamente válido como fallback.
 *  - O MP envia automaticamente um e-mail de confirmação para o e-mail do
 *    pagador. Para evitar spam no cliente, usamos um e-mail fictício único
 *    por cobrança (baseado no UUID da cobrança).
 */
@Slf4j
@Service
public class MercadoPagoService {

    /**
     * CPF padrão matematicamente válido — usado quando o PDV não tem o CPF do cliente.
     * Dígitos verificadores corretos: 04364885665
     */
    private static final String CPF_PADRAO = "04364885665";

    /**
     * Domínio fictício para os e-mails enviados ao MP.
     * Não pode ser igual ao e-mail da conta do tenant no MP.
     */
    private static final String EMAIL_DOMINIO_FICTICIO = "noreply.pdvsaas.com.br";

    /**
     * Gera uma cobrança PIX no Mercado Pago usando as credenciais do tenant.
     *
     * Retorna um record com: mpPaymentId, qrCode, qrCodeBase64, ticketUrl.
     * O mpPaymentId deve ser persistido na CobrancaPix para rastreamento no webhook.
     */
    public PixPaymentResult gerarCobrancaPix(ConfiguracaoPix config,
                                              BigDecimal valor,
                                              String descricao,
                                              String cobrancaId,
                                              int expiracaoMinutos)
            throws MPException, MPApiException {

        // Cada tenant usa suas próprias credenciais — isolamento multi-tenant
        MPRequestOptions options = MPRequestOptions.builder()
                .accessToken(config.getMpAccessToken())
                .build();

        // E-mail fictício único por cobrança — evita spam para o cliente
        String emailFicticio = "cobranca-" +
                cobrancaId.replace("-", "").substring(0, 12) +
                "@" + EMAIL_DOMINIO_FICTICIO;

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .transactionAmount(valor)
                .description(formatarDescricao(
                        descricao != null ? descricao : config.getDescricaoPadrao()))
                .paymentMethodId("pix")
                // externalReference vincula o pagamento MP à nossa CobrancaPix
                .externalReference(cobrancaId)
                .dateOfExpiration(
                        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expiracaoMinutos))
                .payer(PaymentPayerRequest.builder()
                        // MP exige e-mail no pagador — usamos fictício para não spammar o cliente
                        .email(emailFicticio)
                        .firstName("Cliente")
                        .lastName("PDV")
                        // MP exige CPF válido para cobranças PIX no Brasil
                        .identification(IdentificationRequest.builder()
                                .type("CPF")
                                .number(CPF_PADRAO)
                                .build())
                        .build())
                .build();

        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.create(request, options);

            log.info("Cobrança PIX criada no MP — paymentId: {} | status: {} | valor: R$ {} | tenant: {}",
                    payment.getId(), payment.getStatus(), valor, config.getTenant().getId());

            String qrCode = payment.getPointOfInteraction()
                    .getTransactionData().getQrCode();
            String qrCodeBase64 = payment.getPointOfInteraction()
                    .getTransactionData().getQrCodeBase64();
            String ticketUrl = payment.getPointOfInteraction()
                    .getTransactionData().getTicketUrl();

            return new PixPaymentResult(
                    String.valueOf(payment.getId()),
                    qrCode,
                    qrCodeBase64,
                    ticketUrl
            );

        } catch (MPApiException e) {
            log.error("Erro API Mercado Pago — status: {} | body: {}",
                    e.getStatusCode(),
                    e.getApiResponse() != null ? e.getApiResponse().getContent() : "null");
            throw e;
        } catch (MPException e) {
            log.error("Erro de comunicação com Mercado Pago: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Consulta o status de um pagamento no MP pelo ID.
     * Chamado no webhook e no polling de status.
     */
    public Payment buscarPagamento(String mpPaymentId, String accessToken)
            throws MPException, MPApiException {

        MPRequestOptions options = MPRequestOptions.builder()
                .accessToken(accessToken)
                .build();

        try {
            PaymentClient client = new PaymentClient();
            return client.get(Long.parseLong(mpPaymentId), options);
        } catch (MPApiException e) {
            log.error("Erro API ao buscar pagamento {} no MP — status: {} | body: {}",
                    mpPaymentId, e.getStatusCode(),
                    e.getApiResponse() != null ? e.getApiResponse().getContent() : "null");
            throw e;
        }
    }

    /**
     * Valida se o Access Token está correto tentando uma consulta inofensiva.
     * Retorna true se o token é válido (mesmo que o pagamento 0 não exista — 404 = token ok).
     */
    public boolean validarCredencial(String accessToken) {
        try {
            MPRequestOptions options = MPRequestOptions.builder()
                    .accessToken(accessToken)
                    .build();
            new PaymentClient().get(0L, options);
            return true;
        } catch (MPApiException e) {
            // 404 = token válido mas pagamento não existe (esperado)
            // 400 = token válido mas ID inválido (esperado)
            int code = e.getStatusCode();
            return code == 404 || code == 400;
        } catch (Exception e) {
            log.warn("Credencial MP inválida — {}", e.getMessage());
            return false;
        }
    }

    /**
     * O MP limita a descrição a 60 caracteres e não aceita alguns caracteres especiais.
     */
    private String formatarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) return "Pagamento PDV";
        // Remove caracteres não ASCII que o MP rejeita
        String limpa = descricao.replaceAll("[^\\x20-\\x7E]", " ").trim();
        return limpa.length() > 60 ? limpa.substring(0, 60) : limpa;
    }

    /**
     * Record com o resultado da geração de cobrança PIX no MP.
     * mpPaymentId é o ID do pagamento no MP — DEVE ser persistido na CobrancaPix.
     */
    public record PixPaymentResult(
            String mpPaymentId,
            String qrCode,
            String qrCodeBase64,
            String ticketUrl
    ) {}
}
