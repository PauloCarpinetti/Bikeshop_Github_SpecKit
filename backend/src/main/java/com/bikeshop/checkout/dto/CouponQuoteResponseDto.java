package com.bikeshop.checkout.dto;

import java.math.BigDecimal;

public record CouponQuoteResponseDto(String codigo, String tipo, BigDecimal valorDesconto) {
}
