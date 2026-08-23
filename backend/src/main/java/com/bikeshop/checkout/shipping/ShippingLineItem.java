package com.bikeshop.checkout.shipping;

import java.math.BigDecimal;

/**
 * Um item do carrinho, em termos de peso/dimensões, para cálculo de frete (FR-005).
 */
public record ShippingLineItem(
        BigDecimal pesoKg,
        BigDecimal alturaCm,
        BigDecimal larguraCm,
        BigDecimal comprimentoCm,
        int quantidade
) {
}
