package com.bikeshop.checkout.shipping;

import java.math.BigDecimal;

/**
 * Resultado de um cálculo de frete.
 *
 * @param estimado {@code true} quando calculado pelo fallback local (sem credenciais reais dos
 *                 Correios configuradas) em vez da API oficial — ver {@link CorreiosShippingProvider}.
 */
public record ShippingQuote(
        String transportadora,
        BigDecimal valor,
        int prazoDias,
        boolean estimado
) {
}
