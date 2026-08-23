package com.bikeshop.checkout.shipping;

import java.util.List;

/**
 * Cálculo de frete por CEP com cubagem/peso volumétrico (FR-005). Único ponto de acoplamento do
 * checkout com a transportadora — permite trocar de provedor sem alterar {@code CheckoutController}.
 */
public interface ShippingProvider {

    ShippingQuote calculate(String cepDestino, List<ShippingLineItem> itens);
}
