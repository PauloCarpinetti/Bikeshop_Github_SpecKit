package com.bikeshop.payments;

import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.orders.Pedido;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Adapter Stripe (cartão de crédito, FR-006). Sem {@code STRIPE_SECRET_KEY} configurada (ambiente
 * atual), retorna uma intenção simulada em vez de chamar a API real — ver
 * {@link SimulatedPaymentSupport}.
 */
@Component
public class StripePaymentAdapter implements PaymentGatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentAdapter.class);

    private final String secretKey;
    private final String webhookSecret;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public StripePaymentAdapter(@Value("${bikeshop.payments.stripe.secret-key:}") String secretKey,
                                 @Value("${bikeshop.payments.stripe.webhook-secret:}") String webhookSecret,
                                 ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.restClient = RestClient.create("https://api.stripe.com");
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public PaymentIntentResult createIntent(Pedido pedido) {
        if (SimulatedPaymentSupport.isBlank(secretKey)) {
            return SimulatedPaymentSupport.simulateIntent(log, getProvider());
        }

        try {
            long amountCents = pedido.getValorTotal().multiply(BigDecimal.valueOf(100)).longValueExact();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", String.valueOf(amountCents));
            body.add("currency", "brl");
            body.add("metadata[pedido_id]", String.valueOf(pedido.getId()));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1/payment_intents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Resposta vazia do Stripe");
            }
            return new PaymentIntentResult(
                    String.valueOf(response.get("id")),
                    String.valueOf(response.get("client_secret")),
                    String.valueOf(response.get("status")),
                    false
            );
        } catch (Exception ex) {
            log.warn("Falha ao criar payment intent no Stripe, retornando simulação: {}", ex.getMessage());
            return SimulatedPaymentSupport.simulateIntent(log, getProvider());
        }
    }

    @Override
    public PaymentWebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        if (!SimulatedPaymentSupport.isBlank(webhookSecret)) {
            verifySignature(rawPayload, headers.get("Stripe-Signature"));
        }
        try {
            JsonNode object = objectMapper.readTree(rawPayload).path("data").path("object");
            return new PaymentWebhookEvent(object.path("id").asText(), object.path("status").asText());
        } catch (Exception ex) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Payload de webhook do Stripe inválido", HttpStatus.BAD_REQUEST);
        }
    }

    /** Verificação de assinatura conforme o esquema documentado pelo Stripe (header Stripe-Signature). */
    private void verifySignature(String payload, String signatureHeader) {
        if (signatureHeader == null) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Assinatura do webhook Stripe ausente", HttpStatus.BAD_REQUEST);
        }
        try {
            Map<String, String> parts = Arrays.stream(signatureHeader.split(","))
                    .map(part -> part.split("=", 2))
                    .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
            String timestamp = parts.get("t");
            String expectedSignature = parts.get("v1");
            String signedPayload = timestamp + "." + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = HexFormat.of().formatHex(hash);

            if (!computedSignature.equals(expectedSignature)) {
                throw new BusinessException("WEBHOOK_INVALIDO", "Assinatura do webhook Stripe não confere", HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Falha ao validar assinatura do webhook Stripe", HttpStatus.BAD_REQUEST);
        }
    }
}
