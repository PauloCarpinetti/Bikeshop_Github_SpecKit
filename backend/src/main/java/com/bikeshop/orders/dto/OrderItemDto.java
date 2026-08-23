package com.bikeshop.orders.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        Long variacaoProdutoId,
        String sku,
        String nomeProduto,
        BigDecimal precoUnitario,
        int quantidade,
        BigDecimal subtotal
) {
}
