package com.bikeshop.orders.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDto(
        Long id,
        String status,
        BigDecimal valorItens,
        BigDecimal valorFrete,
        BigDecimal valorTotal,
        String transportadora,
        Integer prazoFreteDias,
        String paymentProvider,
        String paymentReference,
        String paymentStatus,
        List<OrderItemDto> itens
) {
}
