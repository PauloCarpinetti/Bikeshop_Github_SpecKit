package com.bikeshop.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartViewDto(
        String cartId,
        List<CartItemViewDto> itens,
        BigDecimal total
) {
}
