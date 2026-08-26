package com.bikeshop.payments;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Comparação de assinaturas/tokens de webhook em tempo constante, evitando timing attack — usada
 * pelos 3 adapters de pagamento em vez de {@code String.equals}.
 */
final class WebhookSignatures {

    private WebhookSignatures() {
    }

    static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
