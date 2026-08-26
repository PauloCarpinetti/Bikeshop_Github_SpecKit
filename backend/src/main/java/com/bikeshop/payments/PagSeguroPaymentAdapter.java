package com.bikeshop.payments;

import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.orders.Pedido;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter PagSeguro/PagBank (boleto/cartão, FR-006). Sem {@code PAGSEGURO_TOKEN} configurado
 * (ambiente atual), retorna uma intenção simulada — ver {@link SimulatedPaymentSupport}.
 *
 * <p>A verificação de webhook usa aqui um token compartilhado simples (header/valor configurado),
 * já que o esquema oficial de assinatura da PagSeguro depende do contrato ativo — revisar quando
 * as credenciais reais estiverem disponíveis.
 */
@Component
public class PagSeguroPaymentAdapter implements PaymentGatewayAdapter {

    private static final Logger log = LoggerFactory.getLogger(PagSeguroPaymentAdapter.class);

    private final String token;
    private final String webhookToken;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PagSeguroPaymentAdapter(@Value("${bikeshop.payments.pagseguro.token:}") String token,
                                    @Value("${bikeshop.payments.pagseguro.webhook-token:}") String webhookToken,
                                    ObjectMapper objectMapper) {
        this.token = token;
        this.webhookToken = webhookToken;
        this.restClient = RestClient.create("https://api.pagseguro.com");
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.PAGSEGURO;
    }

    @Override
    public PaymentIntentResult createIntent(Pedido pedido) {
        if (SimulatedPaymentSupport.isBlank(token)) {
            return SimulatedPaymentSupport.simulateIntent(log, getProvider());
        }

        try {
            long amountCents = pedido.getValorTotal().movePointRight(2).longValueExact();
            Map<String, Object> body = Map.of(
                    "reference_id", "pedido-" + pedido.getId(),
                    "customer", Map.of("name", pedido.getClienteNome(), "email", pedido.getClienteEmail()),
                    "items", List.of(Map.of("name", "Pedido BikeShop", "quantity", 1, "unit_amount", amountCents)),
                    "charges", List.of(Map.of("amount", Map.of("value", amountCents, "currency", "BRL")))
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Resposta vazia da PagSeguro");
            }
            return new PaymentIntentResult(String.valueOf(response.get("id")), null, "PENDING", false);
        } catch (Exception ex) {
            log.warn("Falha ao criar pedido na PagSeguro, retornando simulação: {}", ex.getMessage());
            return SimulatedPaymentSupport.simulateIntent(log, getProvider());
        }
    }

    @Override
    public PaymentWebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        if (!SimulatedPaymentSupport.isBlank(webhookToken)) {
            String received = headers.get("x-webhook-token");
            if (received == null || !WebhookSignatures.constantTimeEquals(received, webhookToken)) {
                throw new BusinessException("WEBHOOK_INVALIDO", "Token do webhook PagSeguro ausente ou inválido", HttpStatus.BAD_REQUEST);
            }
        }
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String id = root.path("id").asText();
            String status = root.path("charges").path(0).path("status").asText();
            return new PaymentWebhookEvent(id, status);
        } catch (Exception ex) {
            throw new BusinessException("WEBHOOK_INVALIDO", "Payload de webhook da PagSeguro inválido", HttpStatus.BAD_REQUEST);
        }
    }
}
