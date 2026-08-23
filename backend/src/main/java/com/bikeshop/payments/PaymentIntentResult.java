package com.bikeshop.payments;

/**
 * @param simulado {@code true} quando não há chave configurada para o gateway (ambiente atual)
 *                 e o resultado foi simulado localmente em vez de vir do provedor real.
 */
public record PaymentIntentResult(
        String reference,
        String redirectUrl,
        String status,
        boolean simulado
) {
}
