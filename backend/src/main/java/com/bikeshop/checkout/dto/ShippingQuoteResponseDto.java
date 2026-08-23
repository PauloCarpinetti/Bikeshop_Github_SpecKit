package com.bikeshop.checkout.dto;

import java.math.BigDecimal;

public record ShippingQuoteResponseDto(
        String transportadora,
        BigDecimal valor,
        int prazoDias,
        boolean estimado
) {
}
