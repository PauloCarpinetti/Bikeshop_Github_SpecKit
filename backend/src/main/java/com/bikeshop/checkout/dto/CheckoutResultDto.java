package com.bikeshop.checkout.dto;

import com.bikeshop.orders.dto.OrderDto;

public record CheckoutResultDto(
        OrderDto pedido,
        String paymentRedirectUrl,
        boolean pagamentoSimulado
) {
}
