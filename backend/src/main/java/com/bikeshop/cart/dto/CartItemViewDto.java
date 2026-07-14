package com.bikeshop.cart.dto;

import java.math.BigDecimal;

public record CartItemViewDto(
        Long variacaoProdutoId,
        String sku,
        String nomeProduto,
        String slugProduto,
        String imagem,
        BigDecimal precoUnitario,
        int quantidade,
        BigDecimal subtotal
) {
}
