package com.bikeshop.payments;

import java.util.UUID;
import org.slf4j.Logger;

/**
 * Comportamento compartilhado pelos adapters de pagamento quando não há chave real configurada
 * (ambiente atual, sem credenciais sandbox) — gera uma intenção de pagamento simulada, sempre
 * "PENDING", em vez de falhar o checkout inteiro. Ver {@link PaymentGatewayAdapter}.
 */
final class SimulatedPaymentSupport {

    private SimulatedPaymentSupport() {
    }

    static PaymentIntentResult simulateIntent(Logger log, PaymentProvider provider) {
        String reference = "SIM-%s-%s".formatted(provider.name(), UUID.randomUUID());
        log.warn("Gateway {} sem credenciais configuradas — gerando intenção de pagamento simulada ({})",
                provider, reference);
        return new PaymentIntentResult(reference, null, "PENDING", true);
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
