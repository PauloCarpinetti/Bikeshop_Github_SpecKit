package com.bikeshop.payments;

import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.orders.Pedido;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter Mercado Pago (PIX/boleto, FR-006). Sem {@code MERCADOPAGO_ACCESS_TOKEN} configurado
 * (ambiente atual), retorna uma intenção simulada — ver {@link SimulatedPaymentSupport}.
 */
@Component
public class MercadoPagoPaymentAdapter implements PaymentGatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoPaymentAdapter.class);

    private final String accessToken;
    private final String webhookSecret;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MercadoPagoPaymentAdapter(@Value("${bikeshop.payments.mercadopago.access-token:}") String accessToken,
                                      @Value("${bikeshop.payments.mercadopago.webhook-secret:}") String webhookSecret,
                                      ObjectMapper objectMapper) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
        this.restClient = RestClient.create("https://api.mercadopago.com");
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MERCADO_PAGO;
    }

    @Override
    public PaymentIntentResult createIntent(Pedido pedido) {
        if (SimulatedPaymentSupport.isBlank(accessToken)) {
            return SimulatedPaymentSupport.simulateIntent(log, getProvider());
        }

        try {
            Map<String, Object> body = Map.of(
                    "transaction_amount", pedido.getValorTotal(),
                    "description", "Pedido BikeShop #" + pedido.getId(),
                    "payment_method_id", "pix",
                    "payer", Map.of("email", pedido.getClienteEmail())
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1/payments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Resposta vazia do Mercado Pago");
            }
            return new PaymentIntentResult(String.valueOf(response.get("id")), null, String.valueOf(response.get("status")), false);
        } catch (Exception ex) {
            log.warn("Falha ao criar pagamento no Mercado Pago, retornando simulação: {}", ex.getMessage());
            return SimulatedPaymentSupport.simulateIntent(log, getProvider());
        }
    }

    @Override
    public PaymentWebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        if (!SimulatedPaymentSupport.isBlank(webhookSecret)) {
            verifySignature(headers);
        }
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String id = root.path("data").path("id").asText();
            String type = root.path("action").asText(root.path("type").asText());
            // A API de notificação não traz o status final no corpo; quem consome o evento consulta
            // o pagamento por id quando necessário. Aqui repassamos o "type/action" como status bruto.
            return new PaymentWebhookEvent(id, type);
        } catch (Exception ex) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Payload de webhook do Mercado Pago inválido", HttpStatus.BAD_REQUEST);
        }
    }

    /** Verificação conforme o esquema x-signature/x-request-id documentado pelo Mercado Pago. */
    private void verifySignature(Map<String, String> headers) {
        String signatureHeader = headers.get("x-signature");
        String requestId = headers.get("x-request-id");
        if (signatureHeader == null || requestId == null) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Assinatura do webhook Mercado Pago ausente", HttpStatus.BAD_REQUEST);
        }
        try {
            Map<String, String> parts = new java.util.HashMap<>();
            for (String part : signatureHeader.split(",")) {
                String[] kv = part.split("=", 2);
                parts.put(kv[0].trim(), kv[1].trim());
            }
            String ts = parts.get("ts");
            String expected = parts.get("v1");
            String manifest = "id:%s;request-id:%s;ts:%s;".formatted(headers.getOrDefault("data-id", ""), requestId, ts);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

            if (!computed.equals(expected)) {
                throw new BusinessException("WEBHOOK_INVALIDO", "Assinatura do webhook Mercado Pago não confere", HttpStatus.BAD_REQUEST);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Falha ao validar assinatura do webhook Mercado Pago", HttpStatus.BAD_REQUEST);
        }
    }
}
