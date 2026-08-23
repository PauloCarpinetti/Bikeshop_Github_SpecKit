package com.bikeshop.orders;

import java.math.BigDecimal;

public record OrderLineItemInput(
        Long variacaoProdutoId,
        String sku,
        String nomeProduto,
        BigDecimal precoUnitario,
        int quantidade
) {
}
